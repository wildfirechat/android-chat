/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
    /** ScrollView wrapper — shown/hidden as co-streamers join/leave */
    private ScrollView coStreamScrollView;
    /** Vertical strip container; tiles added dynamically per co-streamer */
    private LinearLayout coStreamStrip;
    /** Maps userId → its video tile FrameLayout */
    private final Map<String, FrameLayout> coStreamerTiles = new LinkedHashMap<>();
    /** Maps userId → avatar ImageView inside their tile (shown when no video / audio-only) */
    private final Map<String, ImageView> coStreamerAvatars = new LinkedHashMap<>();
    private ImageView hostAvatarImageView;
    private TextView hostNameTextView;
    private TextView liveIndicatorTextView;
    private TextView viewerCountTextView;
    private ImageButton shareButton;
    private ImageButton floatButton;
    private ImageButton closeButton;
    private TextView sayingSomethingView;
    private ImageButton cameraButton;
    private ImageButton recordAudioButton;
    /** Container FrameLayout (id=coStreamButton) that wraps the icon + badge */
    private View coStreamButton;
    private TextView coStreamBadgeView;
    /** Pending request count for badge display */
    private int pendingRequestCount = 0;
    private View loadingView;

    private AVEngineKit engineKit;
    private Conversation conversation;
    private String callId;
    private boolean audioOnly;
    private String pin;
    private String hostUserId;
    private boolean sessionStarted = false;

    private Observer<Object> coStreamRequestObserver;
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** True while the MediaProjection permission dialog is open.
     *  Prevents onStop() from starting the float service (which would kill the launcher callback). */
    private boolean isWaitingForMediaProjectionPermission = false;

    /**
     * Saved from the launcher callback (fires in onStart()) so we can start the capture service
     * in onResume(), AFTER stop() has run. This avoids stop() immediately killing the new service.
     */
    private Intent pendingProjectionData = null;

    private final ActivityResultLauncher<Intent> screenCaptureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                isWaitingForMediaProjectionPermission = false;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Defer to onResume() — stop() must run first to avoid killing the new service.
                    pendingProjectionData = result.getData();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        conversation = getIntent().getParcelableExtra("conversation");

        hostUserId = ChatManager.Instance().getUserId();
        setContentView(R.layout.activity_live_host);

        bindViews();
        bindEvents();
        subscribeCoStreamEvents();

        engineKit = AVEngineKit.Instance();
        AVEngineKit.CallSession session = engineKit.getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            callId = session.getCallId();
            audioOnly = session.isAudioOnly();
            pin = session.getPin();
            session.setCallback(this);
            if (session.getState() == AVEngineKit.CallState.Connected) {
                onSessionConnected();
                // Re-attach local video + restore all co-streamer tiles
                videoContainer.post(() -> session.setupLocalVideoView(videoContainer, SCALING_TYPE));
                restoreCoStreamerTiles(session);
            }
        } else {
            startLive();
        }
    }

    private void attachMessageFragment() {
        if (callId == null) return;
        if (getSupportFragmentManager().findFragmentById(R.id.messageFragmentContainer) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.messageFragmentContainer, LiveMessageFragment.newInstance(callId, false))
                    .commitAllowingStateLoss();
        }
    }

    private void bindViews() {
        videoContainer = findViewById(R.id.videoContainer);
        coStreamScrollView = findViewById(R.id.coStreamScrollView);
        coStreamStrip = findViewById(R.id.coStreamStrip);
        hostAvatarImageView = findViewById(R.id.hostAvatarImageView);
        hostNameTextView = findViewById(R.id.hostNameTextView);
        liveIndicatorTextView = findViewById(R.id.liveIndicatorTextView);
        viewerCountTextView = findViewById(R.id.viewerCountTextView);
        shareButton = findViewById(R.id.shareButton);
        floatButton = findViewById(R.id.floatButton);
        closeButton = findViewById(R.id.closeButton);
        sayingSomethingView = findViewById(R.id.sayingSomethingView);
        cameraButton = findViewById(R.id.cameraButton);
        recordAudioButton = findViewById(R.id.recordAudioButton);
        coStreamButton = findViewById(R.id.coStreamButton);
        coStreamBadgeView = findViewById(R.id.coStreamBadgeView);
        loadingView = findViewById(R.id.loadingView);
    }

    private void bindEvents() {
        closeButton.setOnClickListener(v -> showEndLiveConfirmDialog());
        sayingSomethingView.setOnClickListener(v -> {
            if (callId != null) {
                LiveMessageInputDialogFragment.newInstance(callId)
                        .show(getSupportFragmentManager(), "liveInput");
            }
        });
        cameraButton.setOnClickListener(v -> switchCamera());
        recordAudioButton.setOnClickListener(v -> {
            MediaProjectionManager mgr =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            isWaitingForMediaProjectionPermission = true;
            screenCaptureLauncher.launch(mgr.createScreenCaptureIntent());
        });
        coStreamButton.setOnClickListener(v -> {
            if (callId == null) return;
            // Clear the badge when user opens the manager
            clearCoStreamBadge();
            LiveCoStreamManagerFragment.newInstance(callId)
                    .show(getSupportFragmentManager(), "coStreamManager");
        });
        // Also make the inner icon button clickable
        ImageButton coStreamIconBtn = findViewById(R.id.coStreamIconButton);
        if (coStreamIconBtn != null) {
            coStreamIconBtn.setOnClickListener(v -> coStreamButton.performClick());
        }
        shareButton.setOnClickListener(v -> shareLive());
        floatButton.setOnClickListener(v -> goFloat());
    }

    private void subscribeCoStreamEvents() {
        // Co-stream requests are shown via LiveCoStreamManagerFragment (on-demand)
        // Optionally show a badge/notification when a new request arrives
        coStreamRequestObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (callId != null && callId.equals(content.getCallId())) {
                    runOnUiThread(this::onNewCoStreamRequestReceived);
                }
            }
        };
        LiveDataBus.subscribeForever(LiveStreamingKit.EVENT_CO_STREAM_REQUEST, coStreamRequestObserver);
    }

    private void startLive() {
        try {
            engineKit = AVEngineKit.Instance();
            engineKit.setVideoProfile(VideoProfile.VP720P_3, false);

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
        kit.onHostSessionConnected(callId, audioOnly, pin, hostUserId, title);
        if (conversation != null) {
            kit.sendLiveStartNotification(conversation, title);
        }

        // Start the float service early (without showing the float view) so it's ready for Android 14 FGS requirements
        LiveStreamingService.startForHost(this, title, hostInfo != null ? hostInfo.portrait : null, false);

        attachMessageFragment();

        // Show badge for any pre-existing requests (e.g., when returning from float via notification tap).
        // subscribeCoStreamEvents() runs before callId is set, so LiveDataBus replay is missed.
        // Proactively sync badge count here instead.
        java.util.List<String> existingRequests = LiveStreamingKit.getInstance().getCoStreamRequests();
        if (existingRequests != null) {
            for (int i = 0; i < existingRequests.size(); i++) {
                onNewCoStreamRequestReceived();
            }
        }
    }


    // ---------- Share live ----------

    // ── Co-stream tile management (multi-person) ──────────────────────────────

    /**
     * Restores co-streamer tiles for all current participants when Activity
     * comes back from background (float window). The session is not destroyed
     * in that flow, but SurfaceView renderers must be re-attached.
     */
    private void restoreCoStreamerTiles(AVEngineKit.CallSession session) {
        for (String userId : session.getParticipantIds()) {
            if (userId.equals(hostUserId) || Config.LIVE_STREAMING_ROBOT.equals(userId)) continue;
            if (!coStreamerTiles.containsKey(userId)) {
                addCoStreamerTile(userId, session);
            } else {
                session.setupRemoteVideoView(userId, coStreamerTiles.get(userId), SCALING_TYPE);
            }
        }
    }

    /** Creates a 90×120dp rounded tile for a co-streamer and attaches it to the strip. */
    private void addCoStreamerTile(String userId, AVEngineKit.CallSession session) {
        if (coStreamerTiles.containsKey(userId)) return;

        int tileH = dp2px(120);
        int stripW = dp2px(90);
        int gapPx = coStreamerTiles.isEmpty() ? 0 : dp2px(8);

        FrameLayout tile = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(stripW, tileH);
        lp.topMargin = gapPx;
        tile.setLayoutParams(lp);
        tile.setBackground(getDrawable(R.drawable.live_pip_bg));
        tile.setClipToOutline(true);

        // Avatar placeholder — visible until the co-streamer’s video track arrives
        ImageView avatarView = new ImageView(this);
        FrameLayout.LayoutParams avatarLp = new FrameLayout.LayoutParams(dp2px(48), dp2px(48));
        avatarLp.gravity = android.view.Gravity.CENTER;
        avatarView.setLayoutParams(avatarLp);
        UserInfo userInfo = ChatManager.Instance().getUserInfo(userId, false);
        if (userInfo != null && !TextUtils.isEmpty(userInfo.portrait)) {
            Glide.with(this).load(userInfo.portrait).circleCrop().into(avatarView);
        } else {
            avatarView.setImageResource(R.drawable.live_avatar_placeholder);
        }
        tile.addView(avatarView);

        coStreamStrip.addView(tile);
        coStreamerTiles.put(userId, tile);
        coStreamerAvatars.put(userId, avatarView);
        coStreamScrollView.setVisibility(View.VISIBLE);

        if (session != null) {
            session.setupRemoteVideoView(userId, tile, SCALING_TYPE);
        }
    }

    /** Detaches the renderer and removes the tile for a departing co-streamer. */
    private void removeCoStreamerTile(String userId) {
        FrameLayout tile = coStreamerTiles.remove(userId);
        coStreamerAvatars.remove(userId);
        if (tile == null) return;
        AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
        if (session != null) {
            try {
                session.setupRemoteVideoView(userId, null, SCALING_TYPE);
            } catch (Exception ignored) {
            }
        }
        coStreamStrip.removeView(tile);
        if (coStreamerTiles.isEmpty()) {
            coStreamScrollView.setVisibility(View.GONE);
        }
    }

    private int dp2px(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
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
        LiveStreamingService.startForHost(this, title, hostInfo != null ? hostInfo.portrait : null, true);
        finish();
    }

    private void switchCamera() {
        AVEngineKit.CallSession session = engineKit.getCurrentSession();
        if (session != null) {
            session.switchCamera();
        }
    }

    private void showEndLiveConfirmDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_live_confirm, null);
        ((TextView) dialogView.findViewById(R.id.confirmTitleView)).setText(R.string.live_end_confirm_title);
        ((TextView) dialogView.findViewById(R.id.confirmMessageView)).setText(R.string.live_end_confirm_message);
        ((TextView) dialogView.findViewById(R.id.confirmOkButton)).setText(R.string.live_streaming_end);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(android.view.Gravity.BOTTOM);
            dialog.getWindow().getAttributes().width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        }
        dialogView.findViewById(R.id.confirmCancelButton).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.confirmOkButton).setOnClickListener(v -> {
            dialog.dismiss();
            endLive();
        });
        dialog.show();
    }

    private void onNewCoStreamRequestReceived() {
        pendingRequestCount++;
        if (coStreamBadgeView != null) {
            coStreamBadgeView.setVisibility(View.VISIBLE);
            coStreamBadgeView.setText(pendingRequestCount > 9 ? "9+" : String.valueOf(pendingRequestCount));
        }
        // Brief toast so the host knows without looking at the badge
        Toast.makeText(this, R.string.live_co_stream_new_request, Toast.LENGTH_SHORT).show();
    }

    private void clearCoStreamBadge() {
        pendingRequestCount = 0;
        if (coStreamBadgeView != null) {
            coStreamBadgeView.setVisibility(View.GONE);
        }
    }

    private void endLive() {
        if (sessionStarted) {
            LiveStreamingKit kit = LiveStreamingKit.getInstance();
            if (conversation != null) {
                kit.sendLiveEndNotification(conversation);
            }
            kit.onSessionEnded();
        }
        AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
        if (session != null) {
            session.stopRecordSystemAudio();
            session.leaveConference(true);
        }
        LiveStreamingService.stop(this);
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
            // Re-render surfaces that may have been released while backgrounded
            session.setupLocalVideoView(videoContainer, SCALING_TYPE);
            for (Map.Entry<String, FrameLayout> e : coStreamerTiles.entrySet()) {
                session.setupRemoteVideoView(e.getKey(), e.getValue(), SCALING_TYPE);
            }
        }
        // Hide float view but keep service running
        String title = getString(R.string.live_streaming_default_title);
        UserInfo hostInfo = ChatManager.Instance().getUserInfo(hostUserId, false);
        LiveStreamingService.startForHost(this, title, hostInfo != null ? hostInfo.portrait : null, false);

        if (pendingProjectionData != null) {
            Intent data = pendingProjectionData;
            pendingProjectionData = null;
            LiveStreamingService.startSystemAudioCapture(this, data);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Do NOT go to float mode while the MediaProjection permission dialog is open.
        // That dialog causes onStop() even though the user hasn't explicitly minimised;
        // finishing here would kill the ActivityResultLauncher callback.
        if (isWaitingForMediaProjectionPermission) return;
        AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            if (!isFinishing() && !isChangingConfigurations()) {
                String title = getString(R.string.live_streaming_default_title);
                UserInfo hostInfo = ChatManager.Instance().getUserInfo(hostUserId, false);
                LiveStreamingService.startForHost(this, title, hostInfo != null ? hostInfo.portrait : null, true);
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
        LiveStreamingKit.getInstance().onParticipantActuallyJoined(userId);
        // Create the tile early so it is ready when didReceiveRemoteVideoTrack fires
        runOnUiThread(() -> {
            AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
            addCoStreamerTile(userId, session);
        });
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {
        if (screenSharing || Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        runOnUiThread(() -> {
            removeCoStreamerTile(userId);
            LiveStreamingKit.getInstance().onParticipantLeft(userId);
        });
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        if (screenSharing || Config.LIVE_STREAMING_ROBOT.equals(userId)) return;
        runOnUiThread(() -> {
            AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
            if (session == null) return;
            if (!coStreamerTiles.containsKey(userId)) {
                // Fallback: tile not created in didParticipantJoined yet
                addCoStreamerTile(userId, session);
            } else {
                // Re-attach renderer and hide avatar (video track now available)
                session.setupRemoteVideoView(userId, coStreamerTiles.get(userId), SCALING_TYPE);
                ImageView avatar = coStreamerAvatars.get(userId);
                if (avatar != null) avatar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
        // Tile stays; didParticipantLeft will clean up if the user actually left.
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
        runOnUiThread(() -> {
            ImageView avatar = coStreamerAvatars.get(userId);
            if (avatar != null) avatar.setVisibility(muted ? View.VISIBLE : View.GONE);
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

    // endregion
}
