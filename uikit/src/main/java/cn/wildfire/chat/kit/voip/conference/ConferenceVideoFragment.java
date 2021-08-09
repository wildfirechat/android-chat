/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

public class ConferenceVideoFragment extends BaseConferenceFragment implements AVEngineKit.CallSessionCallback {
    @BindView(R2.id.rootView)
    RelativeLayout rootLinearLayout;

    @BindView(R2.id.topBarView)
    LinearLayout topBarView;

    @BindView(R2.id.bottomPanel)
    FrameLayout bottomPanel;

    @BindView(R2.id.durationTextView)
    TextView durationTextView;

    @BindView(R2.id.manageParticipantTextView)
    TextView manageParticipantTextView;

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

    // TODO 移除，并将VoipBaseActivity.focusVideoUserId 修改为static
    private String focusVideoUserId;
    private ConferenceItem focusConferenceItem;

    private Timer hiddenBarTimer = new Timer();

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;

    public static final String TAG = "ConferenceVideoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference_video_connected, container, false);
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
            List<AVEngineKit.ParticipantProfile> profiles = session.getParticipantProfiles();
            for (AVEngineKit.ParticipantProfile profile : profiles) {
                if (profile.getState() == AVEngineKit.CallState.Connected && !profile.isAudience() && !profile.isVideoMuted()) {
                    didReceiveRemoteVideoTrack(profile.getUserId());
                }
            }
            if (!session.videoMuted) {
                if (session.isLocalVideoCreated()) {
                    didCreateLocalVideoTrack();
                }
            }
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

        updateControlStatus();

        manageParticipantTextView.setText("管理(" + (session.getParticipantIds().size() + 1) + ")");
        rootLinearLayout.setOnClickListener(clickListener);
        startHideBarTimer();
    }

    private void updateControlStatus() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
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
            shareScreenImageView.setSelected(session.isScreenSharing());
        }
    }

    private void initParticipantsView(AVEngineKit.CallSession session) {

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        participantGridView.removeAllViews();

        participants = new ArrayList();
        List<AVEngineKit.ParticipantProfile> profiles = session.getParticipantProfiles();
        if (profiles != null && !profiles.isEmpty()) {
            for (AVEngineKit.ParticipantProfile profile : profiles) {
                if (!profile.isAudience()) {
                    participants.add(profile.getUserId());
                }
            }
        }

        AVEngineKit.ParticipantProfile myProfile = session.getMyProfile();
        if (!myProfile.isAudience()) {
            participants.add(myProfile.getUserId());
        }
        focusConferenceItem = null;

        if (participants.size() > 0) {
            focusVideoUserId = participants.get(0);
            ((VoipBaseActivity) getActivity()).setFocusVideoUserId(focusVideoUserId);
            List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participants);
            for (UserInfo userInfo : participantUserInfos) {
                ConferenceItem conferenceItem = new ConferenceItem(getActivity());
                conferenceItem.setTag(userInfo.uid);


                conferenceItem.getStatusTextView().setText(R.string.connecting);
                conferenceItem.setOnClickListener(clickListener);
                GlideApp.with(conferenceItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(conferenceItem.getPortraitImageView());

                // self
                if (userInfo.uid.equals(focusVideoUserId)) {
                    conferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    focusConferenceItem = conferenceItem;
                    session.setupLocalVideoView(conferenceItem, scalingType);

                } else {
                    conferenceItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));
                    participantGridView.addView(conferenceItem);
                    session.setupRemoteVideoView(userInfo.uid, conferenceItem, scalingType);
                }
            }
        }

        if (focusConferenceItem != null) {
            focusVideoContainerFrameLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            focusVideoContainerFrameLayout.addView(focusConferenceItem);
        }
    }

    private ConferenceItem createSelfView(int with, int height) {
        ConferenceItem multiCallItem = new ConferenceItem(getActivity());
        multiCallItem.setTag(me.uid);
        multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with, height));
        multiCallItem.getStatusTextView().setText(me.displayName);
        multiCallItem.setOnClickListener(clickListener);
        GlideApp.with(multiCallItem).load(me.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
        return multiCallItem;
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {
        String meUid = userViewModel.getUserId();
        ConferenceItem item = rootLinearLayout.findViewWithTag(meUid);
        if (item != null) {
            if (session.videoMuted) {
                item.getStatusTextView().setVisibility(View.VISIBLE);
                item.getStatusTextView().setText("关闭摄像头");
            }
        }

        for (String userId : session.getParticipantIds()) {
            item = rootLinearLayout.findViewWithTag(userId);
            if (item == null) {
                continue;
            }
            PeerConnectionClient client = session.getClient(userId);
            if (client.state == AVEngineKit.CallState.Connected) {
                item.getStatusTextView().setVisibility(View.GONE);
            } else if (client.videoMuted) {
                item.getStatusTextView().setText("关闭摄像头");
                item.getStatusTextView().setVisibility(View.VISIBLE);
            }
        }
    }


    @OnClick(R2.id.minimizeImageView)
    void minimize() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
//        session.stopVideoSource();
        List<String> participants = session.getParticipantIds();
        for (String participant : participants) {
            session.setupRemoteVideoView(participant, null, scalingType);
        }
        ((ConferenceActivity) getActivity()).showFloatingView(focusVideoUserId);
    }

    @OnClick(R2.id.manageParticipantView)
    void addParticipant() {
        ((ConferenceActivity) getActivity()).showParticipantList();
    }

    @OnClick(R2.id.muteView)
    void mute() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            muteImageView.setSelected(!session.isEnableAudio());
            session.muteAudio(session.isEnableAudio());
            startHideBarTimer();
        }
    }

    @OnClick(R2.id.switchCameraImageView)
    void switchCamera() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            session.switchCamera();
            startHideBarTimer();
        }
    }

    @OnClick(R2.id.videoView)
    void video() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            videoImageView.setSelected(session.videoMuted);
            session.muteVideo(!session.videoMuted);
            startHideBarTimer();
        }
    }

    @OnClick(R2.id.hangupView)
    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            if (ChatManager.Instance().getUserId().equals(session.getHost())) {
                new AlertDialog.Builder(getActivity())
                    .setMessage("请选择是否结束会议")
                    .setIcon(R.mipmap.ic_launcher)
                    .setNeutralButton("退出会议", (dialogInterface, i) -> {
                        if (session.getState() != AVEngineKit.CallState.Idle)
                            session.leaveConference(false);
                    })
                    .setPositiveButton("结束会议", (dialogInterface, i) -> {
                        if (session.getState() != AVEngineKit.CallState.Idle)
                            session.leaveConference(true);
                    })
                    .create()
                    .show();
            } else {
                session.leaveConference(false);
            }
        }
    }

    @OnClick(R2.id.shareScreenView)
    void shareScreen() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            shareScreenImageView.setSelected(!session.isScreenSharing());
            if (!session.isScreenSharing()) {
                ((VoipBaseActivity) getActivity()).startScreenShare();
            } else {
                ((VoipBaseActivity) getActivity()).stopScreenShare();
            }
//            List<String> participants = session.getParticipantIds();
//
//            tiny = !tiny;
//            session.switchStream(participants.get(0), tiny);
        }
    }

//    private boolean tiny = false;

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
            if (callSession != null && (callSession.getEndReason() == AVEngineKit.CallEndReason.RoomNotExist || callSession.getEndReason() == AVEngineKit.CallEndReason.RoomParticipantsFull)) {
                // do nothing
            } else {
                getActivity().finish();
            }
        }
    }

    @Override
    public void didParticipantJoined(String userId) {
        if (participants.contains(userId) || userId.equals(focusVideoUserId)) {
            return;
        }

        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }

        manageParticipantTextView.setText("管理(" + (session.getParticipantIds().size() + 1) + ")");

        AVEngineKit.ParticipantProfile profile = session.getParticipantProfile(userId);
        if (profile == null || profile.isAudience()) {
            return;
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        participantGridView.getLayoutParams().height = with;

        UserInfo userInfo = userViewModel.getUserInfo(userId, false);
        ConferenceItem conferenceItem = new ConferenceItem(getActivity());
        conferenceItem.setTag(userInfo.uid);
        conferenceItem.getStatusTextView().setText(userInfo.displayName);
        conferenceItem.setOnClickListener(clickListener);
        GlideApp.with(conferenceItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(conferenceItem.getPortraitImageView());
        session.setupRemoteVideoView(userInfo.uid, conferenceItem, scalingType);

        if (focusVideoUserId == null) {
            conferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            focusConferenceItem = conferenceItem;
            focusVideoUserId = userId;
            ((VoipBaseActivity) getActivity()).setFocusVideoUserId(focusVideoUserId);

            focusVideoContainerFrameLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            focusVideoContainerFrameLayout.addView(focusConferenceItem);
        } else {
            conferenceItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));
            participantGridView.addView(conferenceItem);
        }
        participants.add(userId);
        startHideBarTimer();
    }

    @Override
    public void didParticipantConnected(String userId) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason) {
        removeParticipantView(userId);
        Toast.makeText(getActivity(), ChatManager.Instance().getUserDisplayName(userId) + "离开了会议", Toast.LENGTH_SHORT).show();

        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }
        manageParticipantTextView.setText("管理(" + (session.getParticipantIds().size() + 1) + ")");

        if (focusVideoUserId == null) {
            bottomPanel.setVisibility(View.VISIBLE);
            topBarView.setVisibility(View.VISIBLE);
        }
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
        } else {
            didParticipantJoined(userId);
        }
        updateControlStatus();
    }

    @Override
    public void didMuteStateChanged(List<String> participants) {
        for (String participant : participants) {
            Log.e(TAG, "didMuteStateChanged" + participant);
            AVEngineKit.ParticipantProfile profile = AVEngineKit.Instance().getCurrentSession().getParticipantProfile(participant);
            ConferenceItem item = rootLinearLayout.findViewWithTag(participant);
            if (item != null) {
                if (profile.isVideoMuted()) {
                    item.getStatusTextView().setVisibility(View.VISIBLE);
                    item.getStatusTextView().setText("用户关闭摄像头");

                } else if (profile.isAudioMuted()) {
                    item.getStatusTextView().setVisibility(View.VISIBLE);
                    item.getStatusTextView().setText("用户静音");
                } else {
                    item.getStatusTextView().setVisibility(View.GONE);
                }

                View surfaceView = item.findViewWithTag("v_" + participant);
                if (profile.isVideoMuted()) {
                    if (surfaceView == null) {
                        if (me.uid.equals(participant)) {
                            didCreateLocalVideoTrack();
                        } else {
                            didReceiveRemoteVideoTrack(participant);
                        }
                    } else {
                        surfaceView.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (surfaceView != null) {
                        surfaceView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    @Override
    public void didCreateLocalVideoTrack() {
//        ConferenceItem item = rootLinearLayout.findViewWithTag(me.uid);
//        if (item == null) {
//            DisplayMetrics dm = getResources().getDisplayMetrics();
//            int with = dm.widthPixels;
//            item = createSelfView(with / 3, with / 3);
//            participantGridView.addView(item);
//        }
//
//        if (item.findViewWithTag("v_" + me.uid) != null) {
//            return;
//        }
//
//        SurfaceView surfaceView = getEngineKit().getCurrentSession().createRendererView();
//        if (surfaceView != null && getEngineKit().getCurrentSession() != null) {
//            surfaceView.setZOrderMediaOverlay(false);
//            surfaceView.setTag("v_" + me.uid);
//            item.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//            getEngineKit().getCurrentSession().setupLocalVideoView(item, scalingType);
//        }
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId) {
//        ConferenceItem item = rootLinearLayout.findViewWithTag(userId);
//        if (item == null) {
//            didParticipantJoined(userId);
//        }
//        item = rootLinearLayout.findViewWithTag(userId);
//        SurfaceView surfaceView = item.findViewWithTag("v_" + userId);
//        if (surfaceView == null) {
//            surfaceView = getEngineKit().getCurrentSession().createRendererView();
//            surfaceView.setZOrderMediaOverlay(false);
//            surfaceView.setTag("v_" + userId);
//            item.addView(surfaceView);
//        }
//
//        getEngineKit().getCurrentSession().setupRemoteVideo(userId, surfaceView, scalingType);
//        bringParticipantVideoFront();
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
        // do nothing
        //removeParticipantView(userId);
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
        // do nothing
        // 会议版时，不会走到这儿
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
            if (userId != null && !userId.equals(focusVideoUserId)) {
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
                }
                focusVideoContainerFrameLayout.addView(clickedConferenceItem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                focusConferenceItem = clickedConferenceItem;
                focusVideoUserId = userId;
                ((VoipBaseActivity) getActivity()).setFocusVideoUserId(focusVideoUserId);

                bringParticipantVideoFront();
                if (bottomPanel.getVisibility() == View.GONE) {
                    bottomPanel.setVisibility(View.VISIBLE);
                    topBarView.setVisibility(View.VISIBLE);
                    startHideBarTimer();
                }
            } else {
                if (bottomPanel.getVisibility() == View.GONE) {
                    bottomPanel.setVisibility(View.VISIBLE);
                    topBarView.setVisibility(View.VISIBLE);
                    startHideBarTimer();
                } else {
                    bottomPanel.setVisibility(View.GONE);
                    topBarView.setVisibility(View.GONE);
                }
            }
        }
    };

    private void startHideBarTimer() {
        cancelHideBarTimer();
        if (bottomPanel.getVisibility() == View.GONE) {
            return;
        }

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(() -> {
                    AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
                    if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
                        bottomPanel.setVisibility(View.GONE);
                        topBarView.setVisibility(View.GONE);
                    }
                });
            }
        };

        hiddenBarTimer.schedule(task, 3000);
    }

    private void cancelHideBarTimer() {
        if (hiddenBarTimer != null)
            hiddenBarTimer.cancel();

        hiddenBarTimer = new Timer();
    }

    private void bringParticipantVideoFront() {
        if (focusConferenceItem == null) {
            return;
        }

        SurfaceView focusSurfaceView = findSurfaceView(focusConferenceItem);
        if (focusSurfaceView != null) {
            focusSurfaceView.setZOrderOnTop(false);
            focusSurfaceView.setZOrderMediaOverlay(false);
        }
        int count = participantGridView.getChildCount();
        for (int i = 0; i < count; i++) {
            ConferenceItem callItem = (ConferenceItem) participantGridView.getChildAt(i);
            SurfaceView surfaceView = findSurfaceView(callItem);
            if (surfaceView != null) {
                surfaceView.setZOrderMediaOverlay(true);
                surfaceView.setZOrderOnTop(true);
            }
        }
    }

    private SurfaceView findSurfaceView(ConferenceItem item) {
        for (int i = 0; i < item.getChildCount(); i++) {
            View view = item.getChildAt(i);
            if (view instanceof SurfaceView) {
                return (SurfaceView) view;
            }
        }
        return null;
    }

    private Handler handler = new Handler();

    private void updateCallDuration() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            String text;
            if (session.getConnectedTime() == 0) {
                text = "未开始";
            } else {
                long s = System.currentTimeMillis() - session.getConnectedTime();
                s = s / 1000;
                if (s > 3600) {
                    text = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
                } else {
                    text = String.format("%02d:%02d", s / 60, (s % 60));
                }
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
                    .onPositive((dialog1, which) -> changeMode(AVEngineKit.Instance().getCurrentSession().getCallId(), audience))
                    .onNegative((dialog12, which) -> {
                        // do nothing
                    })
                    .show();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (hiddenBarTimer != null) {
            hiddenBarTimer.cancel();
            hiddenBarTimer = null;
        }
    }
}
