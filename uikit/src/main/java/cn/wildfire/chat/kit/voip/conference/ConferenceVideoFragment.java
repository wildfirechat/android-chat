/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Activity;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceVideoFragment extends Fragment implements AVEngineKit.CallSessionCallback, ConferenceManager.ConferenceManagerEventCallback {
    @BindView(R2.id.rootView)
    RelativeLayout rootLinearLayout;
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
    @BindView(R2.id.shareScreenImageView)
    ImageView shareScreenImageView;


    private List<String> participants;
    private UserInfo me;
    private UserViewModel userViewModel;

    private String focusVideoUserId;
    private ConferenceItem focusConferenceItem;

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;

    public static final String TAG = "ConferenceVideoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference_connected, container, false);
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

        ConferenceManager.Instance().setCallback(this);

        updateControlStatus();
    }

    private void updateControlStatus() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if(session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }

        muteImageView.setEnabled(!session.isAudience());
        videoImageView.setEnabled(!session.isAudience());
        shareScreenImageView.setEnabled(!session.isAudience());

        if (session.isAudience()) {
            muteImageView.setSelected(false);
            videoImageView.setSelected(false);
            shareScreenImageView.setSelected(false);
        } else {
            muteImageView.setSelected(session.isEnableAudio());
            videoImageView.setSelected(!session.videoMuted);
            shareScreenImageView.setSelected(!session.isScreenSharing());
        }
    }

    private void initParticipantsView(AVEngineKit.CallSession session) {
        me = userViewModel.getUserInfo(userViewModel.getUserId(), false);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        participantGridView.removeAllViews();

        participants = session.getParticipantIds();
        if (participants != null && participants.size() > 0) {
            List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participants);
            for (UserInfo userInfo : participantUserInfos) {
                ConferenceItem multiCallItem = new ConferenceItem(getActivity());
                multiCallItem.setTag(userInfo.uid);

                multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));
                multiCallItem.getStatusTextView().setText(R.string.connecting);
                multiCallItem.setOnClickListener(clickListener);
                GlideApp.with(multiCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
                participantGridView.addView(multiCallItem);
            }
        }

        ConferenceItem multiCallItem = createSelfView(-1, -1);

        focusVideoContainerFrameLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        focusVideoContainerFrameLayout.addView(multiCallItem);
        focusConferenceItem = multiCallItem;
        focusVideoUserId = me.uid;
    }

    private ConferenceItem createSelfView(int with, int height) {
        ConferenceItem multiCallItem = new ConferenceItem(getActivity());
        multiCallItem.setTag(me.uid);
        multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with, height));
        multiCallItem.getStatusTextView().setText(me.displayName);
        GlideApp.with(multiCallItem).load(me.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
        return multiCallItem;
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {
        int count = participantGridView.getChildCount();
        String meUid = userViewModel.getUserId();
        for (int i = 0; i < count; i++) {
            View view = participantGridView.getChildAt(i);
            String userId = (String) view.getTag();
            if (meUid.equals(userId)) {
                ((ConferenceItem) view).getStatusTextView().setVisibility(View.GONE);
            } else {
                PeerConnectionClient client = session.getClient(userId);
                if (client.state == AVEngineKit.CallState.Connected) {
                    ((ConferenceItem) view).getStatusTextView().setVisibility(View.GONE);
                } else if (client.videoMuted) {
                    ((ConferenceItem) view).getStatusTextView().setText("关闭摄像头");
                    ((ConferenceItem) view).getStatusTextView().setVisibility(View.VISIBLE);
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
        ((ConferenceActivity) getActivity()).showFloatingView(null);
    }

    @OnClick(R2.id.manageParticipantImageView)
    void addParticipant() {
        ((ConferenceActivity) getActivity()).showParticipantList();
    }

    @OnClick(R2.id.muteImageView)
    void mute() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            muteImageView.setSelected(!session.isEnableAudio());
            session.muteAudio(session.isEnableAudio());
        }
    }

    @OnClick(R2.id.switchCameraImageView)
    void switchCamera() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            session.switchCamera();
        }
    }

    @OnClick(R2.id.videoImageView)
    void video() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            videoImageView.setSelected(session.videoMuted);
            session.muteVideo(!session.videoMuted);
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
        if (session != null) {
            shareScreenImageView.setSelected(session.isScreenSharing());
            if (!session.isScreenSharing()) {
                ((VoipBaseActivity) getActivity()).startScreenShare();
            } else {
                ((VoipBaseActivity) getActivity()).stopScreenShare();
            }
        }
    }

    // hangup 触发
    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        // do nothing
    }

    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
        AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
        if (callState == AVEngineKit.CallState.Connected) {
            updateParticipantStatus(callSession);
            updateControlStatus();
        } else if (callState == AVEngineKit.CallState.Idle) {
            getActivity().finish();
        }
    }

    @Override
    public void didParticipantJoined(String userId) {
        if (participants.contains(userId)) {
            return;
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        participantGridView.getLayoutParams().height = with;

        UserInfo userInfo = userViewModel.getUserInfo(userId, false);
        ConferenceItem multiCallItem = new ConferenceItem(getActivity());
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
        removeParticipantView(userId);
        Toast.makeText(getActivity(), ChatManager.Instance().getUserDisplayName(userId) + "离开了会议", Toast.LENGTH_SHORT).show();
    }

    private void removeParticipantView(String userId) {
        View view = participantGridView.findViewWithTag(userId);
        if (view != null) {
            participantGridView.removeView(view);
        }
        participants.remove(userId);

        if (userId.equals(focusVideoUserId)) {
            focusVideoUserId = null;
            focusVideoContainerFrameLayout.removeView(focusConferenceItem);
            focusConferenceItem = null;

            View item = participantGridView.getChildAt(0);
            if (item != null) {
                item.callOnClick();
            }
        }
    }

    @Override
    public void didChangeMode(boolean audioOnly) {

    }

    @Override
    public void didChangeType(String userId, boolean audience) {
        if (audience) {
            removeParticipantView(userId);
            Toast.makeText(getActivity(), ChatManager.Instance().getUserDisplayName(userId) + "结束了互动", Toast.LENGTH_SHORT).show();
        }
        updateControlStatus();
    }

    @Override
    public void didCreateLocalVideoTrack() {
        ConferenceItem item = rootLinearLayout.findViewWithTag(me.uid);
        if (item == null) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int with = dm.widthPixels;
            item = createSelfView(with / 3, with / 3);
            participantGridView.addView(item);
        }

        if (item.findViewWithTag("v_" + me.uid) != null) {
            return;
        }

        SurfaceView surfaceView = getEngineKit().getCurrentSession().createRendererView();
        if (surfaceView != null) {
            surfaceView.setZOrderMediaOverlay(false);
            surfaceView.setTag("v_" + me.uid);
            item.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            getEngineKit().getCurrentSession().setupLocalVideo(surfaceView, scalingType);
        }
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId) {
        ConferenceItem item = rootLinearLayout.findViewWithTag(userId);
        if (item == null) {
            didParticipantJoined(userId);
        }
        item = rootLinearLayout.findViewWithTag(userId);

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
        removeParticipantView(userId);
    }

    @Override
    public void didError(String reason) {
        Toast.makeText(getActivity(), "发生错误" + reason, Toast.LENGTH_SHORT).show();
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.finish();
        }
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
        updateControlStatus();
    }

    private ConferenceItem getUserConferenceItem(String userId) {
        return participantGridView.findViewWithTag(userId);
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        Log.d(TAG, userId + " volume " + volume);
        ConferenceItem multiCallItem = getUserConferenceItem(userId);
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

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {

    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String userId = (String) v.getTag();
            if (!userId.equals(focusVideoUserId)) {
                ConferenceItem clickedConferenceItem = (ConferenceItem) v;
                int clickedIndex = participantGridView.indexOfChild(v);
                participantGridView.removeView(clickedConferenceItem);
                participantGridView.endViewTransition(clickedConferenceItem);

                clickedConferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                if (focusConferenceItem != null) {
                    focusVideoContainerFrameLayout.removeView(focusConferenceItem);
                    focusVideoContainerFrameLayout.endViewTransition(focusConferenceItem);
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    int with = dm.widthPixels;
                    participantGridView.addView(focusConferenceItem, clickedIndex, new FrameLayout.LayoutParams(with / 3, with / 3));
                    focusConferenceItem.setOnClickListener(clickListener);
                }
                focusVideoContainerFrameLayout.addView(clickedConferenceItem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                clickedConferenceItem.setOnClickListener(null);
                focusConferenceItem = clickedConferenceItem;
                focusVideoUserId = userId;

                bringParticipantVideoFront();
            } else {
                // do nothing

            }
        }
    };

    private void bringParticipantVideoFront() {
        SurfaceView focusSurfaceView = focusConferenceItem.findViewWithTag("v_" + focusVideoUserId);
        if (focusSurfaceView != null) {
            focusSurfaceView.setZOrderOnTop(false);
            focusSurfaceView.setZOrderMediaOverlay(false);
        }
        int count = participantGridView.getChildCount();
        for (int i = 0; i < count; i++) {
            ConferenceItem callItem = (ConferenceItem) participantGridView.getChildAt(i);
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

    @Override
    public void onChangeModeRequest(String conferenceId, boolean audience) {
        if (AVEngineKit.Instance().getCurrentSession() != null
            && AVEngineKit.Instance().getCurrentSession().isConference()
            && AVEngineKit.Instance().getCurrentSession().getCallId().equals(conferenceId)) {
            if (audience) {
                AVEngineKit.Instance().getCurrentSession().switchAudience(true);
                didRemoveRemoteVideoTrack(me.uid);
            } else {
                new MaterialDialog.Builder(getActivity())
                    .content("主持人邀请你参与互动")
                    .positiveText("接受")
                    .negativeText("忽略")
                    .cancelable(false)
                    .onPositive((dialog1, which) -> ConferenceManager.Instance().changeModel(AVEngineKit.Instance().getCurrentSession().getCallId(), audience))
                    .onNegative((dialog12, which) -> {
                        // do nothing
                    })
                    .show();
            }
        }
    }

    @Override
    public void onKickoffRequest(String conferenceId) {
        if (AVEngineKit.Instance().getCurrentSession() != null
            && AVEngineKit.Instance().getCurrentSession().isConference()
            && AVEngineKit.Instance().getCurrentSession().getCallId().equals(conferenceId)) {
            AVEngineKit.Instance().getCurrentSession().leaveConference(false);
        }
    }
}
