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
import androidx.lifecycle.Observer;


import com.bumptech.glide.Glide;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class SingleVideoFragment extends Fragment implements AVEngineKit.CallSessionCallback {

    FrameLayout pipVideoContainer;
    FrameLayout fullscreenVideoContainer;
    ViewGroup outgoingActionContainer;
    ViewGroup incomingActionContainer;
    ViewGroup connectedActionContainer;
    ViewGroup inviteeInfoContainer;
    ImageView portraitImageView;
    ImageView muteAudioImageView;
    TextView nameTextView;
    TextView descTextView;
    TextView durationTextView;
    TextView shareScreenTextView;
    TextView sdkTipTextView;

    private String focusUserId;
    private String targetId;
    private AVEngineKit gEngineKit;

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

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
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.acceptImageView).setOnClickListener(_v -> accept());
        view.findViewById(R.id.incomingAudioOnlyImageView).setOnClickListener(_v -> audioAccept());
        view.findViewById(R.id.outgoingAudioOnlyImageView).setOnClickListener(_v -> audioCall());
        view.findViewById(R.id.connectedAudioOnlyImageView).setOnClickListener(_v -> audioCall());
        view.findViewById(R.id.muteAudioImageView).setOnClickListener(_v -> muteAudio());
        view.findViewById(R.id.connectedHangupImageView).setOnClickListener(_v -> hangUp());
        view.findViewById(R.id.outgoingHangupImageView).setOnClickListener(_v -> hangUp());
        view.findViewById(R.id.incomingHangupImageView).setOnClickListener(_v -> hangUp());
        view.findViewById(R.id.switchCameraImageView).setOnClickListener(_v -> switchCamera());
        view.findViewById(R.id.shareScreenImageView).setOnClickListener(_v -> shareScreen());
        view.findViewById(R.id.fullscreen_video_view).setOnClickListener(_v -> toggleCallControlVisibility());
        view.findViewById(R.id.minimizeImageView).setOnClickListener(_v -> minimize());
        view.findViewById(R.id.pip_video_view).setOnClickListener(_v -> setSwappedFeeds());
    }

    private void bindViews(View view) {
        pipVideoContainer = view.findViewById(R.id.pip_video_view);
        fullscreenVideoContainer = view.findViewById(R.id.fullscreen_video_view);
        outgoingActionContainer = view.findViewById(R.id.outgoingActionContainer);
        incomingActionContainer = view.findViewById(R.id.incomingActionContainer);
        connectedActionContainer = view.findViewById(R.id.connectedActionContainer);
        inviteeInfoContainer = view.findViewById(R.id.inviteeInfoContainer);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        muteAudioImageView = view.findViewById(R.id.muteAudioImageView);
        nameTextView = view.findViewById(R.id.nameTextView);
        descTextView = view.findViewById(R.id.descTextView);
        durationTextView = view.findViewById(R.id.durationTextView);
        shareScreenTextView = view.findViewById(R.id.shareScreenTextView);
        sdkTipTextView = view.findViewById(R.id.sdkTipTextView);
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
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        // 设置 videoProcessor
//        session.setBigVideoProcessor(new TestVideoProcessor());
    }

    @Override
    public void didRemoveRemoteVideoTrack(String s) {

    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        session.setupLocalVideoView(pipVideoContainer, scalingType);
        session.setupRemoteVideoView(targetId, fullscreenVideoContainer, scalingType);
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
//        Log.d(TAG, "voip audio " + userId + " " + volume);
    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {

    }

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

    public void audioAccept() {
        ((SingleCallActivity) getActivity()).audioAccept();
    }

    public void audioCall() {
        ((SingleCallActivity) getActivity()).audioCall();
    }

    public void muteAudio() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        boolean toMute = !session.isAudioMuted();
        session.muteAudio(toMute);
        muteAudioImageView.setSelected(toMute);
    }

    // callFragment.OnCallEvents interface implementation.
    public void hangUp() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null) {
            session.endCall();
        } else {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    public void switchCamera() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();

        if (session != null && !session.isScreenSharing() && session.getState() == AVEngineKit.CallState.Connected) {
            session.switchCamera();
        }
    }

    void shareScreen() {
        if (!AVEngineKit.isSupportConference()) {
            Toast.makeText(getActivity(), getString(R.string.conference_not_supported), Toast.LENGTH_SHORT).show();
            return;
        }
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session == null || session.getState() != AVEngineKit.CallState.Connected) {
            return;
        }
        if (!session.isScreenSharing()) {
            Toast.makeText(getContext(), getString(R.string.conf_screen_share_hint), Toast.LENGTH_LONG).show();
            session.muteAudio(false);
            session.muteVideo(true);

            ((VoipBaseActivity) getActivity()).startScreenShare();
            if (session.isAudience()) {
                session.switchAudience(false);
            }
        } else {
            ((VoipBaseActivity) getActivity()).stopScreenShare();
        }
    }

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
        if (session == null || AVEngineKit.CallState.Idle == session.getState() || session.getParticipantIds() == null || session.getParticipantIds().isEmpty()) {
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
                shareScreenTextView.setText(R.string.stop_screen_sharing);
            } else {
                shareScreenTextView.setText(R.string.start_screen_sharing);
            }

            session.setupLocalVideoView(pipVideoContainer, scalingType);
            session.setupRemoteVideoView(targetId, fullscreenVideoContainer, scalingType);

            muteAudioImageView.setSelected(session.isAudioMuted());
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
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(targetId, false);
        if (userInfo == null) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        userViewModel.userInfoLiveData().observe(getViewLifecycleOwner(), new Observer<List<UserInfo>>() {
            @Override
            public void onChanged(List<UserInfo> userInfos) {
                for (UserInfo info : userInfos) {
                    if (info.uid.equals(targetId)) {
                        updateTargetUserInfoViews(info);
                        break;
                    }
                }
            }
        });
        updateTargetUserInfoViews(userInfo);

        updateCallDuration();

        if(Config.SHOW_DEBUG_INFO && sdkTipTextView != null){
            StringBuilder sb = new StringBuilder();
            String tip = AVEngineKit.isSupportConference() ? "当前使用高级版音视频 SDK": "当前使用多人版音视频 SDK";
            sb.append(tip);
            sdkTipTextView.setText(sb.toString());
        }
    }

    private void updateTargetUserInfoViews(UserInfo userInfo) {
        Glide.with(this).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);
        nameTextView.setText(ChatManager.Instance().getUserDisplayName(userInfo));
    }

    private Handler handler = new Handler();

    private void updateCallDuration() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            long s = ChatManager.Instance().getServerTimestamp() - session.getConnectedTime();
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
