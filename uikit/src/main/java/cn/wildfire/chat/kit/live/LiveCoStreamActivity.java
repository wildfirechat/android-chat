/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;

/**
 * 连麦页面（观众侧）
 * <p>
 * 观众同意连麦后进入此页面。显示主播的远端视频（全屏）和自己的本地预览（PiP）。
 * 实现 AVEngineKit.CallSessionCallback，自行管理视频渲染。
 * </p>
 */
public class LiveCoStreamActivity extends AppCompatActivity implements AVEngineKit.CallSessionCallback {

    private static final RendererCommon.ScalingType SCALING_TYPE = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

    private FrameLayout remoteVideoContainer;
    private FrameLayout localVideoContainer;
    private ImageButton micButton;
    private ImageButton cameraButton;
    private ImageButton endButton;

    private AVEngineKit engineKit;
    private String hostUserId;
    private boolean isMuted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_live_co_stream);

        hostUserId = getIntent().getStringExtra("hostUserId");

        try {
            engineKit = AVEngineKit.Instance();
        } catch (Exception e) {
            Toast.makeText(this, R.string.live_co_stream_join_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        bindEvents();
        setupVideo();
    }

    private void bindViews() {
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer);
        localVideoContainer = findViewById(R.id.localVideoContainer);
        micButton = findViewById(R.id.micButton);
        cameraButton = findViewById(R.id.cameraButton);
        endButton = findViewById(R.id.endButton);
    }

    private void bindEvents() {
        micButton.setOnClickListener(v -> toggleMic());
        cameraButton.setOnClickListener(v -> switchCamera());
        endButton.setOnClickListener(v -> endCoStream());
    }

    private void setupVideo() {
        AVEngineKit.CallSession session = engineKit.getCurrentSession();
        if (session == null) return;
        session.setCallback(this);

        // Local preview (self)
        session.setupLocalVideoView(localVideoContainer, SCALING_TYPE);

        // Remote: host's video
        if (hostUserId != null) {
            session.setupRemoteVideoView(hostUserId, remoteVideoContainer, SCALING_TYPE);
        }
    }

    private void toggleMic() {
        AVEngineKit.CallSession session = engineKit.getCurrentSession();
        if (session == null) return;
        isMuted = !isMuted;
        session.muteAudio(isMuted);
        micButton.setSelected(isMuted);
    }

    private void switchCamera() {
        AVEngineKit.CallSession session = engineKit.getCurrentSession();
        if (session != null) session.switchCamera();
    }

    private void endCoStream() {
        AVEngineKit.CallSession session = engineKit.getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            session.endCall();
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            session.setCallback(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AVEngineKit.CallSession session = engineKit != null ? engineKit.getCurrentSession() : null;
        if (session != null) {
            // Detach video views so host PiP clears
            session.setupLocalVideoView(null, SCALING_TYPE);
            if (hostUserId != null) {
                session.setupRemoteVideoView(hostUserId, null, SCALING_TYPE);
            }
            if (session.getState() != AVEngineKit.CallState.Idle) {
                session.endCall();
            }
        }
    }

    // region AVEngineKit.CallSessionCallback

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        runOnUiThread(this::finish);
    }

    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
        if (callState == AVEngineKit.CallState.Connected) {
            runOnUiThread(this::setupVideo);
        }
    }

    @Override
    public void didCreateLocalVideoTrack() {
        runOnUiThread(() -> {
            AVEngineKit.CallSession session = engineKit.getCurrentSession();
            if (session != null) {
                session.setupLocalVideoView(localVideoContainer, SCALING_TYPE);
            }
        });
    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {}

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {}

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {}

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        if (!screenSharing && userId != null && userId.equals(hostUserId)) {
            runOnUiThread(() -> {
                AVEngineKit.CallSession session = engineKit.getCurrentSession();
                if (session != null) {
                    session.setupRemoteVideoView(userId, remoteVideoContainer, SCALING_TYPE);
                }
            });
        }
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {}

    @Override
    public void didError(String reason) {
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.live_co_stream_join_failed, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void didGetStats(StatsReport[] statsReports) {}

    @Override
    public void didVideoMuted(String userId, boolean muted) {}

    @Override
    public void didReportAudioVolume(String userId, int volume) {}

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {}

    @Override
    public void didChangeMode(boolean audioOnly) {}

    // endregion
}
