/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class SingleVideoFragment extends Fragment implements AVEngineKit.CallSessionCallback {

    @BindView(R2.id.pip_video_view)
    FrameLayout pipVideoContainer;
    @BindView(R2.id.fullscreen_video_view)
    FrameLayout fullscreenVideoContainer;
    @BindView(R2.id.outgoingActionContainer)
    ViewGroup outgoingActionContainer;
    @BindView(R2.id.incomingActionContainer)
    ViewGroup incomingActionContainer;
    @BindView(R2.id.connectedActionContainer)
    ViewGroup connectedActionContainer;
    @BindView(R2.id.inviteeInfoContainer)
    ViewGroup inviteeInfoContainer;
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.descTextView)
    TextView descTextView;
    @BindView(R2.id.durationTextView)
    TextView durationTextView;
    @BindView(R2.id.shareScreenTextView)
    TextView shareScreenTextView;

    private String focusUserId;
    private String targetId;
    private AVEngineKit gEngineKit;

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT;

    private boolean callControlVisible = true;

    private Toast logToast;
    private static final String TAG = "VideoFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_p2p_video_layout, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason reason) {
        // never called
    }

    @Override
    public void didChangeState(AVEngineKit.CallState state) {
        if (state == AVEngineKit.CallState.Connected) {
            incomingActionContainer.setVisibility(View.GONE);
            outgoingActionContainer.setVisibility(View.GONE);
            connectedActionContainer.setVisibility(View.VISIBLE);
            inviteeInfoContainer.setVisibility(View.GONE);
        } else if (state == AVEngineKit.CallState.Idle) {
            if (getActivity() == null) {
                return;
            }
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantLeft(String s, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {

    }

    @Override
    public void didChangeMode(boolean audioOnly) {
        if (audioOnly) {
            gEngineKit.getCurrentSession().setupLocalVideoView(null, scalingType);
            gEngineKit.getCurrentSession().setupRemoteVideoView(targetId, null, scalingType);
        }
    }

    @Override
    public void didCreateLocalVideoTrack() {
    }

    @Override
    public void didRemoveRemoteVideoTrack(String s) {

    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
    }

    @Override
    public void didError(String error) {

    }

    @Override
    public void didGetStats(StatsReport[] reports) {
        // TODO
    }

    @Override
    public void didVideoMuted(String s, boolean b) {

    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        Log.d(TAG, "voip audio " + userId + " " + volume);
    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {

    }

    @OnClick(R2.id.acceptImageView)
    public void accept() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session == null) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().finish();
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            return;
        }
        if (session.getState() == AVEngineKit.CallState.Incoming) {
            session.answerCall(false);
        }
    }

    @OnClick({R2.id.incomingAudioOnlyImageView})
    public void audioAccept() {
        ((SingleCallActivity) getActivity()).audioAccept();
    }

    @OnClick({R2.id.outgoingAudioOnlyImageView, R2.id.connectedAudioOnlyImageView})
    public void audioCall() {
        ((SingleCallActivity) getActivity()).audioCall();
    }

    // callFragment.OnCallEvents interface implementation.
    @OnClick({R2.id.connectedHangupImageView,
        R2.id.outgoingHangupImageView,
        R2.id.incomingHangupImageView})
    public void hangUp() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null) {
            session.endCall();
        } else {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @OnClick(R2.id.switchCameraImageView)
    public void switchCamera() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();

        if (session != null && !session.isScreenSharing() && session.getState() == AVEngineKit.CallState.Connected) {
            session.switchCamera();
        }
    }

    @OnClick(R2.id.shareScreenImageView)
    void shareScreen() {
        if (!AVEngineKit.isSupportConference() && !AVEngineKit.isSupportMultiCall()) {
            Toast.makeText(getActivity(), "该版本不支持屏幕共享", Toast.LENGTH_SHORT).show();
            return;
        }
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session == null || session.getState() != AVEngineKit.CallState.Connected) {
            return;
        }
        if (!session.isScreenSharing()) {
            shareScreenTextView.setText("结束屏幕共享");
            ((VoipBaseActivity) getActivity()).startScreenShare();
        } else {
            ((VoipBaseActivity) getActivity()).stopScreenShare();
            shareScreenTextView.setText("开始屏幕共享");
        }
    }

    @OnClick(R2.id.fullscreen_video_view)
    void toggleCallControlVisibility() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session == null || session.getState() != AVEngineKit.CallState.Connected) {
            return;
        }
        callControlVisible = !callControlVisible;
        if (callControlVisible) {
            connectedActionContainer.setVisibility(View.VISIBLE);
        } else {
            connectedActionContainer.setVisibility(View.GONE);
        }
        // TODO animation
    }

    @OnClick(R2.id.minimizeImageView)
    public void minimize() {
//        gEngineKit.getCurrentSession().stopVideoSource();
        ((SingleCallActivity) getActivity()).showFloatingView(null);
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    @OnClick(R2.id.pip_video_view)
    void setSwappedFeeds() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null) {
            if (focusUserId == null || focusUserId.equals(targetId)) {
                session.setupLocalVideoView(fullscreenVideoContainer, scalingType);
                session.setupRemoteVideoView(targetId, pipVideoContainer, scalingType);
                focusUserId = ChatManager.Instance().getUserId();
            } else {
                session.setupLocalVideoView(pipVideoContainer, scalingType);
                session.setupRemoteVideoView(targetId, fullscreenVideoContainer, scalingType);
                focusUserId = targetId;
            }
        }

        // pls refer to https://github.com/google/ExoPlayer/issues/7999
        ViewGroup p = (ViewGroup) pipVideoContainer.getParent();
        p.removeView(fullscreenVideoContainer);
        p.removeView(pipVideoContainer);
        ChatManager.Instance().getMainHandler().post(() -> {
            p.addView(fullscreenVideoContainer, 0);
            p.addView(pipVideoContainer, 1);
        });
    }

    private void init() {
        gEngineKit = ((SingleCallActivity) getActivity()).getEngineKit();
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session == null || AVEngineKit.CallState.Idle == session.getState()) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else if (AVEngineKit.CallState.Connected == session.getState()) {
            incomingActionContainer.setVisibility(View.GONE);
            outgoingActionContainer.setVisibility(View.GONE);
            connectedActionContainer.setVisibility(View.VISIBLE);
            inviteeInfoContainer.setVisibility(View.GONE);

            targetId = session.getParticipantIds().get(0);
            focusUserId = targetId;

            if (session.isScreenSharing()) {
                shareScreenTextView.setText("结束屏幕共享");
            } else {
                shareScreenTextView.setText("开始屏幕共享");
            }

            session.setupLocalVideoView(pipVideoContainer, scalingType);
            session.setupRemoteVideoView(targetId, fullscreenVideoContainer, scalingType);
        } else {
            targetId = session.getParticipantIds().get(0);
            focusUserId = ChatManager.Instance().getUserId();

            session.setupLocalVideoView(fullscreenVideoContainer, scalingType);
            session.setupRemoteVideoView(targetId, pipVideoContainer, scalingType);

            if (session.getState() == AVEngineKit.CallState.Outgoing) {
                incomingActionContainer.setVisibility(View.GONE);
                outgoingActionContainer.setVisibility(View.VISIBLE);
                connectedActionContainer.setVisibility(View.GONE);
                descTextView.setText(R.string.av_waiting);
                gEngineKit.getCurrentSession().startPreview();
            } else {
                incomingActionContainer.setVisibility(View.VISIBLE);
                outgoingActionContainer.setVisibility(View.GONE);
                connectedActionContainer.setVisibility(View.GONE);
                descTextView.setText(R.string.av_video_invite);
            }
        }
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(targetId, false);
        if (userInfo == null) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        GlideApp.with(this).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);
        nameTextView.setText(userViewModel.getUserDisplayName(userInfo));

        updateCallDuration();
    }

    private Handler handler = new Handler();

    private void updateCallDuration() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            long s = System.currentTimeMillis() - session.getConnectedTime();
            s = s / 1000;
            String text;
            if (s > 3600) {
                text = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
            } else {
                text = String.format("%02d:%02d", s / 60, (s % 60));
            }
            durationTextView.setText(text);
        }
        handler.postDelayed(this::updateCallDuration, 1000);
    }
}
