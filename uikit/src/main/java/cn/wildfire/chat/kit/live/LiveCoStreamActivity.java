/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.LiveStreamingStartMessageContent;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 观众连麦页面
 * <p>
 * 从 LiveAudienceActivity 收到连麦邀请/接受后跳转此页面，加入 WebRTC 会议。
 * 支持多人连麦：主播全屏，自己 + 其他连麦者以 PiP 小块显示在右侧。
 * 连麦结束后 finish()，返回 LiveAudienceActivity 继续观看 HLS。
 * </p>
 */
public class LiveCoStreamActivity extends FragmentActivity implements AVEngineKit.CallSessionCallback {

    private static final RendererCommon.ScalingType SCALING_TYPE = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

    /** Full-screen container for the host's video */
    private FrameLayout remoteVideoContainer;
    /** Avatar shown in host container when audio-only / no video */
    private ImageView focusAvatarView;
    /** PiP strip scroll view — contains local + remote thumbnails */
    private ScrollView pipScrollView;
    /** Vertical strip inside pipScrollView */
    private LinearLayout pipStrip;
    /** End co-stream button */
    private ImageButton endCoStreamButton;

    /** Self video tile — always first in pipStrip */
    private FrameLayout localTile;
    /** Avatar inside localTile, shown until local video track arrives */
    private ImageView localAvatarView;
    /** userId → thumbnail tile for other co-streamers (not host) */
    private final Map<String, FrameLayout> remoteThumbnails = new LinkedHashMap<>();
    /** userId → avatar view inside that tile */
    private final Map<String, ImageView> remoteAvatars = new LinkedHashMap<>();

    private LiveCoStreamContent coStreamContent;
    private LiveStreamingStartMessageContent liveContent;
    private String hostUserId;
    /** True when the audience chose audio-only co-stream — we never show a local camera feed. */
    private boolean audioOnlyMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_live_co_stream);

        coStreamContent = getIntent().getParcelableExtra("coStreamContent");
        liveContent = getIntent().getParcelableExtra("liveContent");

        if (coStreamContent == null) {
            finish();
            return;
        }

        hostUserId = coStreamContent.getHost();
        audioOnlyMode = coStreamContent.isAudioOnlyRequest();

        bindViews();
        bindEvents();

        // Load host portrait into the focus avatar (shown until video track arrives)
        UserInfo hostInfo = ChatManager.Instance().getUserInfo(hostUserId, false);
        if (hostInfo != null && !TextUtils.isEmpty(hostInfo.portrait)) {
            Glide.with(this).load(hostInfo.portrait).circleCrop().into(focusAvatarView);
        }

        // Add chat overlay
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.messageFragmentContainer,
                        LiveMessageFragment.newInstance(coStreamContent.getCallId()))
                .commitAllowingStateLoss();

        // Create the local self tile
        createLocalTile();

        // If returning from float window, reuse existing session instead of re-joining
        AVEngineKit.CallSession existing = AVEngineKit.Instance().getCurrentSession();
        if (existing != null && existing.getState() != AVEngineKit.CallState.Idle
                && coStreamContent.getCallId().equals(existing.getCallId())) {
            existing.setCallback(this);
            if (!audioOnlyMode && localTile != null) {
                existing.setupLocalVideoView(localTile, SCALING_TYPE);
                if (localAvatarView != null) localAvatarView.setVisibility(View.GONE);
            }
            existing.setupRemoteVideoView(hostUserId, remoteVideoContainer, SCALING_TYPE);
            focusAvatarView.setVisibility(View.GONE);
            for (String uid : existing.getParticipantIds()) {
                if (uid.equals(hostUserId)
                        || uid.equals(ChatManager.Instance().getUserId())
                        || Config.LIVE_STREAMING_ROBOT.equals(uid)) continue;
                if (!remoteThumbnails.containsKey(uid)) {
                    addRemoteThumbnail(uid);
                    existing.setupRemoteVideoView(uid, remoteThumbnails.get(uid), SCALING_TYPE);
                }
            }
        } else {
            // Join the conference fresh
            AVEngineKit.CallSession session = LiveStreamingKit.getInstance()
                    .joinConferenceForCoStream(coStreamContent, this);
            if (session == null) {
                Toast.makeText(this, R.string.live_co_stream_join_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void bindViews() {
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer);
        focusAvatarView = findViewById(R.id.focusAvatarView);
        pipScrollView = findViewById(R.id.pipScrollView);
        pipStrip = findViewById(R.id.pipStrip);
        endCoStreamButton = findViewById(R.id.endCoStreamButton);
    }

    private void bindEvents() {
        endCoStreamButton.setOnClickListener(v -> endCoStream());
    }

    private void endCoStream() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            session.endCall();
        }
        returnToAudienceActivity();
    }

    /** After co-stream ends, relaunch the HLS audience view. */
    private void returnToAudienceActivity() {
        if (liveContent != null) {
            Intent intent = new Intent(this, LiveAudienceActivity.class);
            intent.putExtra("liveContent", liveContent);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
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
        LiveStreamingFloatService.stop(this);
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            session.setCallback(this);
            // Re-attach renderers (e.g. after screen rotation)
            if (!audioOnlyMode && localTile != null) session.setupLocalVideoView(localTile, SCALING_TYPE);
            if (hostUserId != null) {
                session.setupRemoteVideoView(hostUserId, remoteVideoContainer, SCALING_TYPE);
            }
            for (Map.Entry<String, FrameLayout> e : remoteThumbnails.entrySet()) {
                session.setupRemoteVideoView(e.getKey(), e.getValue(), SCALING_TYPE);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            if (!isFinishing() && !isChangingConfigurations()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(this)) {
                    // No overlay permission — just let the session run in background
                    return;
                }
                String title = coStreamContent.getTitle() != null
                        ? coStreamContent.getTitle() : getString(R.string.live_streaming);
                UserInfo hostInfo = ChatManager.Instance().getUserInfo(hostUserId, false);
                LiveStreamingFloatService.start(this, title, false, true, hostUserId,
                        hostInfo != null ? hostInfo.portrait : null, null, getIntent());
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Renderers are cleaned up by the session itself when endCall() fires.
        // Do NOT null out renderers here: if we're going to float mode, the float
        // service attaches them AFTER onDestroy() runs (service starts are async),
        // so nulling here would leave the float with no video.
    }

    // region ── AVEngineKit.CallSessionCallback ──────────────────────────────

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        runOnUiThread(this::returnToAudienceActivity);
    }

    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
    }

    @Override
    public void didCreateLocalVideoTrack() {
        // In audio-only mode we muted our video — keep the avatar visible instead of
        // attaching a blank renderer surface which would show a transparent rectangle.
        if (audioOnlyMode) return;
        runOnUiThread(() -> {
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session != null && localTile != null) {
                session.setupLocalVideoView(localTile, SCALING_TYPE);
                if (localAvatarView != null) localAvatarView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        if (screenSharing || Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        if (userId.equals(ChatManager.Instance().getUserId())) return;
        if (userId.equals(hostUserId)) {
            // Host is already in the conference — attach renderer now so it's ready for the video track
            runOnUiThread(() -> {
                AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
                if (session != null) {
                    session.setupRemoteVideoView(userId, remoteVideoContainer, SCALING_TYPE);
                }
            });
        } else {
            // Non-host co-streamers get a thumbnail tile
            runOnUiThread(() -> {
                if (!remoteThumbnails.containsKey(userId)) addRemoteThumbnail(userId);
            });
        }
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {
        if (screenSharing || Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        // Attach host renderer early so it's ready when video track arrives
        if (userId.equals(hostUserId)) {
            runOnUiThread(() -> {
                AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
                if (session != null) {
                    session.setupRemoteVideoView(userId, remoteVideoContainer, SCALING_TYPE);
                }
            });
        }
    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {
        if (screenSharing || Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        if (userId.equals(hostUserId)) {
            // Host left — end the co-stream
            runOnUiThread(this::finish);
        } else {
            runOnUiThread(() -> removeRemoteThumbnail(userId));
        }
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        Log.d("jyj", "didReceiveRemoteVideoTrack " + userId + " " + screenSharing);
        if (screenSharing || Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        runOnUiThread(() -> {
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session == null) return;
            if (userId.equals(hostUserId)) {
                // Host video arrived — hide avatar, renderer was already attached in didParticipantConnected
                focusAvatarView.setVisibility(View.GONE);
                session.setupRemoteVideoView(userId, remoteVideoContainer, SCALING_TYPE);
            } else {
                if (!remoteThumbnails.containsKey(userId)) addRemoteThumbnail(userId);
                FrameLayout tile = remoteThumbnails.get(userId);
                ImageView avatar = remoteAvatars.get(userId);
                if (avatar != null) avatar.setVisibility(View.GONE);
                if (tile != null) session.setupRemoteVideoView(userId, tile, SCALING_TYPE);
            }
        });
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
        // Keep tile; tile is removed only when participant actually leaves
    }

    @Override
    public void didError(String reason) {
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.live_co_stream_join_failed, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void didGetStats(StatsReport[] statsReports) {
    }

    @Override
    public void didVideoMuted(String userId, boolean muted) {
        runOnUiThread(() -> {
            String myId = ChatManager.Instance().getUserId();
            if (userId.equals(myId)) {
                if (localAvatarView != null) localAvatarView.setVisibility(muted ? View.VISIBLE : View.GONE);
            } else if (userId.equals(hostUserId)) {
                focusAvatarView.setVisibility(muted ? View.VISIBLE : View.GONE);
            } else {
                ImageView avatar = remoteAvatars.get(userId);
                if (avatar != null) avatar.setVisibility(muted ? View.VISIBLE : View.GONE);
            }
        });
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

    // ── Tile management ──────────────────────────────────────────────────────

    private void createLocalTile() {
        if (localTile != null) return;

        int tileW = dp2px(90);
        int tileH = dp2px(120);

        localTile = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH);
        localTile.setLayoutParams(lp);
        localTile.setBackground(getDrawable(R.drawable.live_pip_bg));
        localTile.setClipToOutline(true);

        // Self avatar — visible until camera starts
        ImageView avatarView = new ImageView(this);
        FrameLayout.LayoutParams avatarLp = new FrameLayout.LayoutParams(dp2px(48), dp2px(48));
        avatarLp.gravity = android.view.Gravity.CENTER;
        avatarView.setLayoutParams(avatarLp);
        UserInfo myInfo = ChatManager.Instance().getUserInfo(ChatManager.Instance().getUserId(), false);
        if (myInfo != null && !TextUtils.isEmpty(myInfo.portrait)) {
            Glide.with(this).load(myInfo.portrait).circleCrop().into(avatarView);
        } else {
            avatarView.setImageResource(R.drawable.live_avatar_placeholder);
        }
        localAvatarView = avatarView;
        localTile.addView(avatarView);

        pipStrip.addView(localTile, 0);
        pipScrollView.setVisibility(View.VISIBLE);
    }

    private void addRemoteThumbnail(String userId) {
        if (remoteThumbnails.containsKey(userId)) return;

        int tileH = dp2px(120);
        int stripW = dp2px(90);

        FrameLayout tile = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(stripW, tileH);
        lp.topMargin = dp2px(8);
        tile.setLayoutParams(lp);
        tile.setBackground(getDrawable(R.drawable.live_pip_bg));
        tile.setClipToOutline(true);

        ImageView avatarView = new ImageView(this);
        FrameLayout.LayoutParams avatarLp = new FrameLayout.LayoutParams(dp2px(48), dp2px(48));
        avatarLp.gravity = android.view.Gravity.CENTER;
        avatarView.setLayoutParams(avatarLp);
        UserInfo info = ChatManager.Instance().getUserInfo(userId, true);
        if (info != null && !TextUtils.isEmpty(info.portrait)) {
            Glide.with(this).load(info.portrait).circleCrop().into(avatarView);
        } else {
            avatarView.setImageResource(R.drawable.live_avatar_placeholder);
        }
        tile.addView(avatarView);

        pipStrip.addView(tile);
        remoteThumbnails.put(userId, tile);
        remoteAvatars.put(userId, avatarView);
    }

    private void removeRemoteThumbnail(String userId) {
        FrameLayout tile = remoteThumbnails.remove(userId);
        remoteAvatars.remove(userId);
        if (tile != null) {
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session != null) {
                try {
                    session.setupRemoteVideoView(userId, null, SCALING_TYPE);
                } catch (Exception ignored) {
                }
            }
            pipStrip.removeView(tile);
        }
    }

    private int dp2px(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
