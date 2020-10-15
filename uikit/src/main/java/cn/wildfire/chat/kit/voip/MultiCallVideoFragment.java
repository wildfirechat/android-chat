/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProviders;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class MultiCallVideoFragment extends Fragment implements AVEngineKit.CallSessionCallback {
    @BindView(R2.id.rootView)
    LinearLayout rootLinearLayout;
    @BindView(R2.id.durationTextView)
    TextView durationTextView;
    @BindView(R2.id.videoContainerGridLayout)
    GridLayout participantGridView;
    @BindView(R2.id.focusVideoContainerFrameLayout)
    FrameLayout focusVideoContainerFrameLayout;
    @BindView(R2.id.muteImageView)
    ImageView muteImageView;
    @BindView(R2.id.videoImageView)
    ImageView videoImageView;

    private List<String> participants;
    private UserInfo me;
    private UserViewModel userViewModel;

    private MultiCallItem focusMultiCallItem;

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;
    private boolean micEnabled = true;
    private boolean videoEnabled = true;

    public static final String TAG = "MultiCallVideoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_video_outgoing_connected, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private AVEngineKit getEngineKit() {
        return AVEngineKit.Instance();
    }

    private void init() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }

        initParticipantsView(session);

        if (session.getState() == AVEngineKit.CallState.Connected) {
            session.startVideoSource();
            List<AVEngineKit.ParticipantProfile> profiles = session.getParticipantProfiles();
            for (AVEngineKit.ParticipantProfile profile : profiles) {
                if (profile.getState() == AVEngineKit.CallState.Connected) {
                    didReceiveRemoteVideoTrack(profile.getUserId());
                }
            }
            didCreateLocalVideoTrack();
        } else {
            if (session.isLocalVideoCreated()) {
                didCreateLocalVideoTrack();
            } else {
                session.startPreview();
            }
        }

        updateCallDuration();
        updateParticipantStatus(session);
        bringParticipantVideoFront();

        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);
    }

    private void initParticipantsView(AVEngineKit.CallSession session) {
        me = userViewModel.getUserInfo(userViewModel.getUserId(), false);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        participantGridView.removeAllViews();

        participants = session.getParticipantIds();

        List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participants);
        List<UserInfo> unfocusedParticipantUserInfos;
        UserInfo focusedUserInfo;
        if (session.isScreenSharing()) {
            unfocusedParticipantUserInfos = participantUserInfos.size() > 1 ? participantUserInfos.subList(1, participantUserInfos.size()) : new ArrayList<>();
            focusedUserInfo = participantUserInfos.get(0);
            unfocusedParticipantUserInfos.add(me);

        } else {
            unfocusedParticipantUserInfos = participantUserInfos;
            focusedUserInfo = me;
        }

        for (UserInfo userInfo : unfocusedParticipantUserInfos) {
            MultiCallItem multiCallItem = new MultiCallItem(getActivity());
            multiCallItem.setTag(userInfo.uid);

            multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));
            multiCallItem.getStatusTextView().setText(R.string.connecting);
            multiCallItem.setOnClickListener(clickListener);
            GlideApp.with(multiCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
            participantGridView.addView(multiCallItem);
        }

        MultiCallItem multiCallItem = new MultiCallItem(getActivity());
        multiCallItem.setTag(focusedUserInfo.uid);
        multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with, with));
        multiCallItem.getStatusTextView().setText(focusedUserInfo.displayName);
        GlideApp.with(multiCallItem).load(focusedUserInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());

        focusVideoContainerFrameLayout.setLayoutParams(new FrameLayout.LayoutParams(with, with));
        focusVideoContainerFrameLayout.addView(multiCallItem);
        focusMultiCallItem = multiCallItem;
        ((VoipBaseActivity) getActivity()).setFocusVideoUserId(focusedUserInfo.uid);
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {
        int count = participantGridView.getChildCount();
        String meUid = userViewModel.getUserId();
        for (int i = 0; i < count; i++) {
            View view = participantGridView.getChildAt(i);
            String userId = (String) view.getTag();
            if (meUid.equals(userId)) {
                ((MultiCallItem) view).getStatusTextView().setVisibility(View.GONE);
            } else {
                PeerConnectionClient client = session.getClient(userId);
                if (client.state == AVEngineKit.CallState.Connected) {
                    ((MultiCallItem) view).getStatusTextView().setVisibility(View.GONE);
                } else if (client.videoMuted) {
                    ((MultiCallItem) view).getStatusTextView().setText("关闭摄像头");
                    ((MultiCallItem) view).getStatusTextView().setVisibility(View.VISIBLE);
                }
            }
        }
    }


    @OnClick(R2.id.minimizeImageView)
    void minimize() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
//        session.stopVideoSource();
        List<String> participants = session.getParticipantIds();
        for (String participant : participants) {
            session.setupRemoteVideo(participant, null, scalingType);
        }
        ((MultiCallActivity) getActivity()).showFloatingView(((VoipBaseActivity) getActivity()).getFocusVideoUserId());
    }

    @OnClick(R2.id.addParticipantImageView)
    void addParticipant() {
        ((MultiCallActivity) getActivity()).addParticipant(AVEngineKit.MAX_VIDEO_PARTICIPANT_COUNT - participants.size() - 1);
    }

    @OnClick(R2.id.muteImageView)
    void mute() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            micEnabled = !micEnabled;
            session.muteAudio(!micEnabled);
            muteImageView.setSelected(!micEnabled);
        }
    }

    @OnClick(R2.id.switchCameraImageView)
    void switchCamera() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && !session.isScreenSharing() && session.getState() == AVEngineKit.CallState.Connected) {
            session.switchCamera();
        }
    }

    @OnClick(R2.id.videoImageView)
    void video() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected && !session.isScreenSharing()) {
            session.muteVideo(!videoEnabled);
            videoEnabled = !videoEnabled;
            videoImageView.setSelected(videoEnabled);
        }
    }

    @OnClick(R2.id.hangupImageView)
    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            session.endCall();
        }
    }


    @OnClick(R2.id.shareScreenImageView)
    void shareScreen() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() != AVEngineKit.CallState.Connected) {
            return;
        }
        if (!session.isScreenSharing()) {
            ((VoipBaseActivity) getActivity()).startScreenShare();
        } else {
            ((VoipBaseActivity) getActivity()).stopScreenShare();
        }
    }

    // hangup 触发
    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        // do nothing
        getActivity().finish();
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
        AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
        if (callState == AVEngineKit.CallState.Connected) {
            updateParticipantStatus(callSession);
        } else if (callState == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    public void didParticipantJoined(String userId) {
        if (participants.contains(userId)) {
            return;
        }
        int count = participantGridView.getChildCount();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        participantGridView.getLayoutParams().height = with;

        UserInfo userInfo = userViewModel.getUserInfo(userId, false);
        MultiCallItem multiCallItem = new MultiCallItem(getActivity());
        multiCallItem.setTag(userInfo.uid);
        multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));
        multiCallItem.getStatusTextView().setText(userInfo.displayName);
        multiCallItem.setOnClickListener(clickListener);
        GlideApp.with(multiCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
        participantGridView.addView(multiCallItem);
        participants.add(userId);
    }

    @Override
    public void didParticipantConnected(String userId) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason) {
        View view = participantGridView.findViewWithTag(userId);
        if (view != null) {
            participantGridView.removeView(view);
        }
        participants.remove(userId);

        if (userId.equals(((VoipBaseActivity) getActivity()).getFocusVideoUserId())) {
            ((VoipBaseActivity) getActivity()).setFocusVideoUserId(null);
            focusVideoContainerFrameLayout.removeView(focusMultiCallItem);
            focusMultiCallItem = null;
        }

        Toast.makeText(getActivity(), ChatManager.Instance().getUserDisplayName(userId) + "离开了通话", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didChangeMode(boolean audioOnly) {

    }

    @Override
    public void didCreateLocalVideoTrack() {
        MultiCallItem item = rootLinearLayout.findViewWithTag(me.uid);
        SurfaceView surfaceView = item.findViewWithTag("v_" + me.uid);

        if (surfaceView == null) {
            surfaceView = getEngineKit().getCurrentSession().createRendererView();
            surfaceView.setZOrderMediaOverlay(false);
            surfaceView.setTag("v_" + me.uid);
            item.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        getEngineKit().getCurrentSession().setupLocalVideo(surfaceView, scalingType);
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId) {
        MultiCallItem item = rootLinearLayout.findViewWithTag(userId);
        if (item == null) {
            return;
        }

        SurfaceView surfaceView = getEngineKit().getCurrentSession().createRendererView();
        if (surfaceView != null) {
            surfaceView.setZOrderMediaOverlay(false);
            item.addView(surfaceView);
            surfaceView.setTag("v_" + userId);
            getEngineKit().getCurrentSession().setupRemoteVideo(userId, surfaceView, scalingType);
        }
        bringParticipantVideoFront();
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
        MultiCallItem item = rootLinearLayout.findViewWithTag(userId);
        if (item != null) {
            View view = item.findViewWithTag("v_" + userId);
            if (view != null) {
                item.removeView(view);
            }

            item.getStatusTextView().setText("关闭摄像头");
            item.getStatusTextView().setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void didError(String reason) {
        Toast.makeText(getActivity(), "发生错误" + reason, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didGetStats(StatsReport[] statsReports) {

    }

    @Override
    public void didVideoMuted(String userId, boolean videoMuted) {
        if (videoMuted) {
            didRemoveRemoteVideoTrack(userId);
        } else {
            if (userId.equals(me.uid)) {
                didCreateLocalVideoTrack();
            } else {
                didReceiveRemoteVideoTrack(userId);
            }
        }
    }

    private MultiCallItem getUserMultiCallItem(String userId) {
        return participantGridView.findViewWithTag(userId);
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        Log.d(TAG, userId + " volume " + volume);
        MultiCallItem multiCallItem = getUserMultiCallItem(userId);
        if (multiCallItem != null) {
            if (volume > 1000) {
                multiCallItem.getStatusTextView().setVisibility(View.VISIBLE);
                multiCallItem.getStatusTextView().setText("正在说话");
            } else {
                multiCallItem.getStatusTextView().setVisibility(View.GONE);
                multiCallItem.getStatusTextView().setText("");
            }
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String userId = (String) v.getTag();
            AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
            if (session == null
                || session.getState() != AVEngineKit.CallState.Connected
                || (userId.equals(ChatManager.Instance().getUserId()) && session.isScreenSharing())) {
                return;
            }

            if (!userId.equals(((VoipBaseActivity) getActivity()).getFocusVideoUserId())) {
                MultiCallItem clickedMultiCallItem = (MultiCallItem) v;
                int clickedIndex = participantGridView.indexOfChild(v);
                participantGridView.removeView(clickedMultiCallItem);
                participantGridView.endViewTransition(clickedMultiCallItem);

                if (focusMultiCallItem != null) {
                    focusVideoContainerFrameLayout.removeView(focusMultiCallItem);
                    focusVideoContainerFrameLayout.endViewTransition(focusMultiCallItem);
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    int with = dm.widthPixels;
                    participantGridView.addView(focusMultiCallItem, clickedIndex, new FrameLayout.LayoutParams(with / 3, with / 3));
                    focusMultiCallItem.setOnClickListener(clickListener);
                }
                focusVideoContainerFrameLayout.addView(clickedMultiCallItem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                clickedMultiCallItem.setOnClickListener(null);
                focusMultiCallItem = clickedMultiCallItem;
                ((VoipBaseActivity) getActivity()).setFocusVideoUserId(userId);


                bringParticipantVideoFront();
            } else {
                // do nothing

            }
        }
    };

    private void bringParticipantVideoFront() {
        SurfaceView focusSurfaceView = focusMultiCallItem.findViewWithTag("v_" + ((VoipBaseActivity) getActivity()).getFocusVideoUserId());
        if (focusSurfaceView != null) {
            focusSurfaceView.setZOrderOnTop(false);
            focusSurfaceView.setZOrderMediaOverlay(false);
        }
        int count = participantGridView.getChildCount();
        for (int i = 0; i < count; i++) {
            MultiCallItem callItem = (MultiCallItem) participantGridView.getChildAt(i);
            SurfaceView surfaceView = callItem.findViewWithTag("v_" + callItem.getTag());
            if (surfaceView != null) {
                surfaceView.setZOrderMediaOverlay(true);
                surfaceView.setZOrderOnTop(true);
            }
        }
    }

    private Handler handler = new Handler();

    private void updateCallDuration() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
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
