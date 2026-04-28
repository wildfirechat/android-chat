/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.LiveStreamingStartMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 观众观看直播页面
 * <p>
 * 观众点击直播消息后进入此页面，通过 HLS 地址播放直播流。
 * 直播地址：Config.LIVE_STREAMING_ADDRESS + callId + ".m3u8"
 * </p>
 */
public class LiveAudienceActivity extends FragmentActivity implements AVEngineKit.CallSessionCallback {
    private FullScreenVideoView videoView;
    private ProgressBar loadingProgressBar;
    private ImageView hostAvatarImageView;
    private TextView hostNameTextView;
    private TextView liveTagTextView;
    private ImageButton shareButton;
    private ImageButton floatButton;
    private ImageButton closeButton;
    private TextView sayingSomethingView;
    private ImageButton requestCoStreamButton;

    // Co-stream overlay views
    private FrameLayout coStreamOverlay;
    private FrameLayout remoteVideoContainer;
    private FrameLayout localVideoContainer;
    private ImageButton endCoStreamButton;

    private static final RendererCommon.ScalingType SCALING_TYPE = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

    private LiveStreamingStartMessageContent liveContent;
    private String coStreamHostUserId;

    private Observer<Object> coStreamInviteObserver;
    private Observer<Object> coStreamAcceptedObserver;
    private Observer<Object> coStreamRejectedObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);


        setContentView(R.layout.activity_live_audience);

        liveContent = getIntent().getParcelableExtra("liveContent");
        if (liveContent == null) {
            finish();
            return;
        }

        bindViews();
        bindEvents();
        subscribeCoStreamEvents();

        AVEngineKit kit = AVEngineKit.Instance();
        AVEngineKit.CallSession session = kit.getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            session.setCallback(this);
            if (session.getState() == AVEngineKit.CallState.Connected) {
                // 如果已经在连麦中，显示连麦界面
                coStreamOverlay.setVisibility(View.VISIBLE);
                session.setupLocalVideoView(localVideoContainer, SCALING_TYPE);
                // 找到主播 ID
                for (String participantId : session.getParticipantIds()) {
                    if (!participantId.equals(ChatManager.Instance().getUserId())) {
                        session.setupRemoteVideoView(participantId, remoteVideoContainer, SCALING_TYPE);
                        break;
                    }
                }
            } else {
                startWatching();
            }
        } else {
            startWatching();
        }
    }

    private void bindViews() {
        videoView = findViewById(R.id.videoView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        hostAvatarImageView = findViewById(R.id.hostAvatarImageView);
        hostNameTextView = findViewById(R.id.hostNameTextView);
        liveTagTextView = findViewById(R.id.liveTagTextView);
        shareButton = findViewById(R.id.shareButton);
        floatButton = findViewById(R.id.floatButton);
        closeButton = findViewById(R.id.closeButton);
        sayingSomethingView = findViewById(R.id.sayingSomethingView);
        requestCoStreamButton = findViewById(R.id.requestCoStreamButton);
        coStreamOverlay = findViewById(R.id.coStreamOverlay);
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer);
        localVideoContainer = findViewById(R.id.localVideoContainer);
        endCoStreamButton = findViewById(R.id.endCoStreamButton);

        // Load host info
        String hostUserId = liveContent.getHost();
        if (!TextUtils.isEmpty(hostUserId)) {
            UserInfo hostInfo = ChatManager.Instance().getUserInfo(hostUserId, false);
            if (hostInfo != null) {
                if (!TextUtils.isEmpty(hostInfo.displayName)) {
                    hostNameTextView.setText(hostInfo.displayName);
                }
                if (!TextUtils.isEmpty(hostInfo.portrait)) {
                    Glide.with(this).load(hostInfo.portrait).circleCrop().into(hostAvatarImageView);
                }
            }
        }
    }

    private void bindEvents() {
        closeButton.setOnClickListener(v -> finish());
        requestCoStreamButton.setOnClickListener(v -> showCoStreamOptions());
        shareButton.setOnClickListener(v -> shareLive());
        floatButton.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.live_permission_float_required, Toast.LENGTH_SHORT).show();
                return;
            }
            String title = liveContent.getTitle() != null ? liveContent.getTitle()
                    : getString(R.string.live_streaming);
            UserInfo hostInfo = ChatManager.Instance().getUserInfo(liveContent.getHost(), false);
            String hlsUrl = LiveStreamingKit.getHlsUrl(liveContent.getCallId());
            LiveStreamingFloatService.start(this, title, false, hostInfo != null ? hostInfo.portrait : null, hlsUrl, getIntent());
            finish();
        });
        sayingSomethingView.setOnClickListener(v -> {
            String callId = liveContent.getCallId();
            if (callId != null) {
                LiveMessageInputDialogFragment.newInstance(callId)
                        .show(getSupportFragmentManager(), "liveInput");
            }
        });
        endCoStreamButton.setOnClickListener(v -> endCoStream());
    }

    private void subscribeCoStreamEvents() {
        // Host invites this audience member to co-stream
        coStreamInviteObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (liveContent.getCallId().equals(content.getCallId())) {
                    showCoStreamInviteDialog(content);
                }
            }
        };
        // Host accepted our request
        coStreamAcceptedObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                // Check callId matches and we're not already co-streaming
                if (liveContent.getCallId().equals(content.getCallId())
                        && coStreamOverlay.getVisibility() != View.VISIBLE) {
                    joinConference(content);
                }
            }
        };
        // Host rejected our request
        coStreamRejectedObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (liveContent.getCallId().equals(content.getCallId())) {
                    runOnUiThread(() -> Toast.makeText(this,
                            R.string.live_co_stream_rejected, Toast.LENGTH_SHORT).show());
                }
            }
        };
        LiveDataBus.subscribeForever(LiveStreamingKit.EVENT_CO_STREAM_INVITE, coStreamInviteObserver);
        LiveDataBus.subscribeForever(LiveStreamingKit.EVENT_CO_STREAM_ACCEPTED, coStreamAcceptedObserver);
        LiveDataBus.subscribeForever(LiveStreamingKit.EVENT_CO_STREAM_REJECTED, coStreamRejectedObserver);
    }

    private void startWatching() {
        String callId = liveContent.getCallId();
        String streamUrl = LiveStreamingKit.getHlsUrl(callId);

        // Attach (or re-attach) the chat fragment — safe to call multiple times
        if (getSupportFragmentManager().findFragmentById(R.id.messageFragmentContainer) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.messageFragmentContainer, LiveMessageFragment.newInstance(callId))
                    .commitAllowingStateLoss();
        }

        loadingProgressBar.setVisibility(View.VISIBLE);

        videoView.setVideoURI(Uri.parse(streamUrl));

        videoView.setOnPreparedListener(mp -> {
            loadingProgressBar.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            liveTagTextView.setVisibility(View.VISIBLE);
            mp.setLooping(false);
            // Feed the real video dimensions so FullScreenVideoView can center-crop correctly
            videoView.setVideoSize(mp.getVideoWidth(), mp.getVideoHeight());
            videoView.start();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            loadingProgressBar.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.live_streaming_load_failed, Toast.LENGTH_SHORT).show();
            return true;
        });

        videoView.setOnCompletionListener(mp -> {
            Toast.makeText(this, R.string.live_streaming_ended, Toast.LENGTH_SHORT).show();
            finish();
        });

        videoView.requestFocus();
        videoView.start();
    }

    // ---------- Co-streaming ----------

    private void shareLive() {
        Message message = new Message();
        message.content = liveContent;
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        Intent intent = new Intent(this, ForwardActivity.class);
        intent.putExtra("messages", messages);
        startActivity(intent);
    }

    private void showCoStreamOptions() {
        LiveCoStreamOptionsFragment.newInstance(liveContent)
                .show(getSupportFragmentManager(), "coStreamOptions");
    }

    private void showCoStreamInviteDialog(LiveCoStreamContent content) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle(R.string.live_co_stream_invite_title)
                .setMessage(R.string.live_co_stream_invite_message)
                .setPositiveButton(R.string.live_co_stream_accept, (d, w) -> joinConference(content))
                .setNegativeButton(R.string.live_co_stream_reject, (d, w) ->
                        LiveStreamingKit.getInstance().rejectCoStreamInvite(content))
                .setCancelable(false)
                .show());
    }

    private void joinConference(LiveCoStreamContent content) {
        // Stop HLS playback while co-streaming
        if (videoView != null) videoView.stopPlayback();

        coStreamHostUserId = content.getHost();
        AVEngineKit.CallSession session = LiveStreamingKit.getInstance().joinConferenceForCoStream(content, this);

        if (session == null) {
            coStreamHostUserId = null;
            Toast.makeText(this, R.string.live_co_stream_join_failed, Toast.LENGTH_SHORT).show();
            if (videoView != null) videoView.start();
            return;
        }
        // Show co-stream overlay in-page
        coStreamOverlay.setVisibility(View.VISIBLE);
    }

    private void endCoStream() {
        AVEngineKit kit = AVEngineKit.Instance();
        if (kit != null) {
            AVEngineKit.CallSession session = kit.getCurrentSession();
            if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
                session.endCall();
            }
        }
        hideCoStreamOverlay();
    }

    private void hideCoStreamOverlay() {
        coStreamHostUserId = null;
        coStreamOverlay.setVisibility(View.GONE);
        // Resume HLS
        startWatching();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WindowInsetsControllerCompat insetsController =
                    new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            insetsController.hide(WindowInsetsCompat.Type.systemBars());
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AVEngineKit kit = AVEngineKit.Instance();
        AVEngineKit.CallSession session = kit.getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            session.setCallback(this);
        }
        LiveStreamingFloatService.stop(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing() && !isChangingConfigurations()) {
            // 如果在直播中或者连麦中，自动进入悬浮窗
            // 这里为了简单，总是进入悬浮窗
            String title = liveContent.getTitle() != null ? liveContent.getTitle()
                    : getString(R.string.live_streaming);
            UserInfo hostInfo = ChatManager.Instance().getUserInfo(liveContent.getHost(), false);
            String hlsUrl = LiveStreamingKit.getHlsUrl(liveContent.getCallId());
            LiveStreamingFloatService.start(this, title, false, hostInfo != null ? hostInfo.portrait : null, hlsUrl, getIntent());
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveDataBus.unsubscribe(LiveStreamingKit.EVENT_CO_STREAM_INVITE, coStreamInviteObserver);
        LiveDataBus.unsubscribe(LiveStreamingKit.EVENT_CO_STREAM_ACCEPTED, coStreamAcceptedObserver);
        LiveDataBus.unsubscribe(LiveStreamingKit.EVENT_CO_STREAM_REJECTED, coStreamRejectedObserver);
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }

    // region ── AVEngineKit.CallSessionCallback ──────────────────────────────

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        runOnUiThread(this::hideCoStreamOverlay);
    }

    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
    }

    @Override
    public void didCreateLocalVideoTrack() {
        runOnUiThread(() -> {
            try {
                AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
                if (session != null && localVideoContainer != null) {
                    session.setupLocalVideoView(localVideoContainer, SCALING_TYPE);
                }
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {
    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        if (screenSharing) return;
        runOnUiThread(() -> {
            try {
                AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
                if (session != null && remoteVideoContainer != null) {
                    session.setupRemoteVideoView(userId, remoteVideoContainer, SCALING_TYPE);
                }
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
    }

    @Override
    public void didError(String reason) {
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.live_co_stream_join_failed, Toast.LENGTH_SHORT).show();
            hideCoStreamOverlay();
        });
    }

    @Override
    public void didGetStats(StatsReport[] statsReports) {
    }

    @Override
    public void didVideoMuted(String userId, boolean muted) {
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {
    }

    @Override
    public void didChangeMode(boolean audioOnly) {
    }

    @Override
    public void didChangeType(String userId, boolean audience, boolean screenSharing) {
    }

    @Override
    public void didMuteStateChanged(List<String> participants) {
    }

    @Override
    public void didMediaLostPacket(String media, int lostPacket, boolean screenSharing) {
    }

    // endregion
}
