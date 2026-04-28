/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.ArrayList;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.VideoProfile;
import cn.wildfirechat.message.LiveStreamingStartMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 主播直播页面
 * <p>
 * 主播开始直播后，此页面显示本地摄像头预览，并提供控制按钮（静音、切换摄像头、结束直播）。
 * 开播时：
 * 1. 调用 startConference 创建会议
 * 2. 向 LIVE_STREAMING_ROBOT 发送消息（使单聊会话，线路1），触发服务端录制推流
 * 3. 向当前会话发送 LiveStreamingStartMessageContent，会话成员点击后可观看直播
 * </p>
 */
public class LiveHostActivity extends FragmentActivity implements AVEngineKit.CallSessionCallback {

    private static final RendererCommon.ScalingType SCALING_TYPE = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

    private FrameLayout videoContainer;
    private FrameLayout coStreamVideoContainer;
    private ImageView hostAvatarImageView;
    private TextView hostNameTextView;
    private TextView liveIndicatorTextView;
    private TextView viewerCountTextView;
    private ImageButton shareButton;
    private ImageButton floatButton;
    private ImageButton closeButton;
    private TextView sayingSomethingView;
    private TextView cameraButton;
    private TextView coStreamButton;
    private View loadingView;

    private AVEngineKit engineKit;
    private Conversation conversation;
    private String callId;
    private String pin;
    private String hostUserId;
    private boolean sessionStarted = false;

    private Observer<Object> coStreamRequestObserver;
    private final Handler handler = new Handler(Looper.getMainLooper());

    protected PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        conversation = getIntent().getParcelableExtra("conversation");
        if (conversation == null) {
            finish();
            return;
        }

        hostUserId = ChatManager.Instance().getUserId();
        setContentView(R.layout.activity_live_host);

        bindViews();
        bindEvents();
        subscribeCoStreamEvents();

        engineKit = AVEngineKit.Instance();
        AVEngineKit.CallSession session = engineKit.getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            callId = session.getCallId();
            pin = session.getPin();
            session.setCallback(this);
            if (session.getState() == AVEngineKit.CallState.Connected) {
                onSessionConnected();
                // 恢复预览
                videoContainer.post(() -> {
                    session.setupLocalVideoView(videoContainer, SCALING_TYPE);
                });
            }
        } else {
            startLive();
        }
    }

    private void attachMessageFragment() {
        if (callId == null) return;
        if (getSupportFragmentManager().findFragmentById(R.id.messageFragmentContainer) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.messageFragmentContainer, LiveMessageFragment.newInstance(callId))
                    .commitAllowingStateLoss();
        }
    }

    private void bindViews() {
        videoContainer = findViewById(R.id.videoContainer);
        coStreamVideoContainer = findViewById(R.id.coStreamVideoContainer);
        hostAvatarImageView = findViewById(R.id.hostAvatarImageView);
        hostNameTextView = findViewById(R.id.hostNameTextView);
        liveIndicatorTextView = findViewById(R.id.liveIndicatorTextView);
        viewerCountTextView = findViewById(R.id.viewerCountTextView);
        shareButton = findViewById(R.id.shareButton);
        floatButton = findViewById(R.id.floatButton);
        closeButton = findViewById(R.id.closeButton);
        sayingSomethingView = findViewById(R.id.sayingSomethingView);
        cameraButton = findViewById(R.id.cameraButton);
        coStreamButton = findViewById(R.id.coStreamButton);
        loadingView = findViewById(R.id.loadingView);
    }

    private void bindEvents() {
        closeButton.setOnClickListener(v -> endLive());
        cameraButton.setOnClickListener(v -> switchCamera());
        coStreamButton.setOnClickListener(v -> {
            if (callId == null) return;
            LiveCoStreamManagerFragment.newInstance(callId)
                    .show(getSupportFragmentManager(), "coStreamManager");
        });
        shareButton.setOnClickListener(v -> shareLive());
        floatButton.setOnClickListener(v -> goFloat());
        sayingSomethingView.setOnClickListener(v -> {
            if (callId != null) {
                LiveMessageInputDialogFragment.newInstance(callId)
                        .show(getSupportFragmentManager(), "liveInput");
            }
        });
    }

    private void subscribeCoStreamEvents() {
        // Co-stream requests are shown via LiveCoStreamManagerFragment (on-demand)
        // Optionally show a badge/notification when a new request arrives
        coStreamRequestObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (callId != null && callId.equals(content.getCallId())) {
                    runOnUiThread(() -> Toast.makeText(this,
                            R.string.live_co_stream_new_request, Toast.LENGTH_SHORT).show());
                }
            }
        };
        LiveDataBus.subscribeForever(LiveStreamingKit.EVENT_CO_STREAM_REQUEST, coStreamRequestObserver);
    }

    private void startLive() {
        try {
            engineKit = AVEngineKit.Instance();
            engineKit.setVideoProfile(VideoProfile.VP720P, false);

        } catch (Exception e) {
            Toast.makeText(this, R.string.live_streaming_start_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Generate callId and pin via SDK
        callId = LiveStreamingKit.generateCallId();
        pin = LiveStreamingKit.generatePin();

        String host = hostUserId;
        String title = getString(R.string.live_streaming_default_title);

        loadingView.setVisibility(View.VISIBLE);

        AVEngineKit.CallSession session = engineKit.startConference(
                callId, false, pin, host, title, "", false, false, false, this);

        if (session == null) {
            Toast.makeText(this, R.string.live_streaming_start_failed, Toast.LENGTH_SHORT).show();
            loadingView.setVisibility(View.GONE);
            finish();
        }
    }

    private void onSessionConnected() {
        if (sessionStarted) return;
        sessionStarted = true;

        loadingView.setVisibility(View.GONE);
        liveIndicatorTextView.setVisibility(View.VISIBLE);
        viewerCountTextView.setVisibility(View.VISIBLE);
        coStreamButton.setVisibility(View.VISIBLE);

        // Load host info
        UserInfo hostInfo = ChatManager.Instance().getUserInfo(hostUserId, false);
        if (hostInfo != null) {
            if (!TextUtils.isEmpty(hostInfo.displayName)) {
                hostNameTextView.setText(hostInfo.displayName);
            }
            if (!TextUtils.isEmpty(hostInfo.portrait)) {
                Glide.with(this).load(hostInfo.portrait).circleCrop().into(hostAvatarImageView);
            }
        }

        String title = getString(R.string.live_streaming_default_title);
        LiveStreamingKit kit = LiveStreamingKit.getInstance();
        kit.onHostSessionConnected(callId, pin, hostUserId, title);
        kit.sendLiveStartNotification(conversation, title);

        attachMessageFragment();
    }


    // ---------- Share live ----------

    private void shareLive() {
        LiveStreamingKit kit = LiveStreamingKit.getInstance();
        LiveSession liveSession = kit.getCurrentSession();
        if (liveSession == null) return;
        LiveStreamingStartMessageContent content = new LiveStreamingStartMessageContent(
                liveSession.callId, liveSession.hostUserId, liveSession.title, "",
                System.currentTimeMillis() / 1000, false, false, false, liveSession.pin, "");
        Message message = new Message();
        message.content = content;
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        Intent intent = new Intent(this, ForwardActivity.class);
        intent.putExtra("messages", messages);
        startActivity(intent);
    }

    // ---------- Float window ----------

    private void goFloat() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.live_permission_float_required, Toast.LENGTH_SHORT).show();
            return;
        }
        String title = getString(R.string.live_streaming_default_title);
        UserInfo hostInfo = ChatManager.Instance().getUserInfo(hostUserId, false);
        LiveStreamingFloatService.start(this, title, true, hostInfo != null ? hostInfo.portrait : null, null, getIntent());
        finish();
    }

    private void switchCamera() {
        AVEngineKit.CallSession session = engineKit.getCurrentSession();
        if (session != null) {
            session.switchCamera();
        }
    }

    private void endLive() {
        if (sessionStarted) {
            LiveStreamingKit kit = LiveStreamingKit.getInstance();
            kit.sendLiveEndNotification(conversation);
            kit.onSessionEnded();
        }
        AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
        if (session != null) {
            session.leaveConference(true);
        }
        finish();
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
        AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            session.setCallback(this);
        }
        LiveStreamingFloatService.stop(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            if (!isFinishing() && !isChangingConfigurations()) {
                String title = getString(R.string.live_streaming_default_title);
                UserInfo hostInfo = ChatManager.Instance().getUserInfo(hostUserId, false);
                LiveStreamingFloatService.start(this, title, true, hostInfo != null ? hostInfo.portrait : null, null, getIntent());
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        LiveDataBus.unsubscribe(LiveStreamingKit.EVENT_CO_STREAM_REQUEST, coStreamRequestObserver);
        // endLive() already called; only clean up if activity is destroyed without proper endLive
        if (sessionStarted && LiveStreamingKit.getInstance().getCurrentSession() == null) {
            LiveStreamingKit.getInstance().onSessionEnded();
        }
    }

    // region AVEngineKit.CallSessionCallback

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        runOnUiThread(this::finish);
    }

    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
        runOnUiThread(() -> {
            if (callState == AVEngineKit.CallState.Connected) {
                onSessionConnected();
            }
        });
    }

    @Override
    public void didCreateLocalVideoTrack() {
        runOnUiThread(() -> {
            AVEngineKit.CallSession session = engineKit.getCurrentSession();
            if (session != null && videoContainer != null) {
                session.setupLocalVideoView(videoContainer, SCALING_TYPE);
            }
        });
    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        if (screenSharing || Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        // Participant actually connected — sync kit state (no extra signal sent)
        LiveStreamingKit.getInstance().onParticipantActuallyJoined(userId);
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {
        if (screenSharing || Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        runOnUiThread(() -> {
            try {
                AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
                if (session != null) {
                    session.setupRemoteVideoView(userId, null, SCALING_TYPE);
                }
            } catch (Exception ignored) {
            }
            if (coStreamVideoContainer != null) {
                coStreamVideoContainer.removeAllViews();
                coStreamVideoContainer.setVisibility(View.GONE);
            }
            LiveStreamingKit.getInstance().onParticipantLeft(userId);
        });
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        // Skip the streaming robot and screen sharing tracks
        if (screenSharing || Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        runOnUiThread(() -> {
            AVEngineKit.CallSession session = engineKit.getCurrentSession();
            if (session != null && coStreamVideoContainer != null) {
                coStreamVideoContainer.setVisibility(View.VISIBLE);
                session.setupRemoteVideoView(userId, coStreamVideoContainer, SCALING_TYPE);
            }
        });
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
        if (Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        runOnUiThread(() -> {
            AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
            if (session != null) {
                // Detach the renderer from this user so the SurfaceView is released
                session.setupRemoteVideoView(userId, null, SCALING_TYPE);
            }
            if (coStreamVideoContainer != null) {
                coStreamVideoContainer.removeAllViews();
                coStreamVideoContainer.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void didError(String reason) {
        runOnUiThread(() -> {
            Toast.makeText(this, getString(R.string.live_streaming_error, reason), Toast.LENGTH_SHORT).show();
            finish();
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

    // endregion
}
