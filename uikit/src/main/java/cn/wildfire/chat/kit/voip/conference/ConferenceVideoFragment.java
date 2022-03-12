/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
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
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfire.chat.kit.voip.VoipCallItem;
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
    private VoipCallItem focusConferenceItem;


    private final RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;
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
        me = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            return;
        }

        initParticipantsView(session);

        if (session.getState() == AVEngineKit.CallState.Outgoing) {
            session.startPreview();
        }

        handler.post(updateCallDurationRunnable);
        updateParticipantStatus(session);

        updateControlStatus();

        manageParticipantTextView.setText("管理(" + (session.getParticipantProfiles().size() + 1) + ")");
        rootLinearLayout.setOnClickListener(clickListener);
        startHideBarTimer();
    }

    private void updateControlStatus() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }

        if (session.isAudience()) {
            muteImageView.setSelected(true);
            videoImageView.setSelected(true);
            shareScreenImageView.setSelected(true);
        } else {
            muteImageView.setSelected(session.isAudioMuted());
            videoImageView.setSelected(session.videoMuted);
            shareScreenImageView.setSelected(session.isScreenSharing());
        }
    }

    private void initParticipantsView(AVEngineKit.CallSession session) {

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = Math.min(dm.widthPixels, dm.heightPixels);

        participantGridView.removeAllViews();

        participants = new ArrayList();
        List<String> participantIds = new ArrayList<>();
        List<AVEngineKit.ParticipantProfile> profiles = session.getParticipantProfiles();
        if (profiles == null) {
            profiles = new ArrayList<>();
        }
        profiles = profiles.stream().filter(p -> !p.isAudience()).collect(Collectors.toList());
        for (AVEngineKit.ParticipantProfile profile : profiles) {
            participants.add(VoipBaseActivity.participantKey(profile.getUserId(), profile.isScreenSharing()));
            participantIds.add(profile.getUserId());
        }

        AVEngineKit.ParticipantProfile myProfile = session.getMyProfile();
        if (!myProfile.isAudience()) {
            participants.add(VoipBaseActivity.participantKey(myProfile.getUserId(), myProfile.isScreenSharing()));
            participantIds.add(myProfile.getUserId());
            profiles.add(myProfile);
        }
        focusConferenceItem = null;

        if (participants.size() > 0) {
            // TODO focusVideoUserId 这个值现在不准，需要由VoipCallService传过来
            AVEngineKit.ParticipantProfile p = profiles.get(0);
            focusVideoUserId = VoipBaseActivity.participantKey(p.getUserId(), p.isScreenSharing());
            ((VoipBaseActivity) getActivity()).setFocusVideoUserId(focusVideoUserId);
            for (AVEngineKit.ParticipantProfile profile : profiles) {
                VoipCallItem conferenceItem = new VoipCallItem(getActivity());
                String participantKey = VoipBaseActivity.participantKey(profile.getUserId(), profile.isScreenSharing());
                UserInfo userInfo = ChatManager.Instance().getUserInfo(profile.getUserId(), false);
                conferenceItem.setTag(participantKey);

                conferenceItem.getStatusTextView().setText(R.string.connecting);
                conferenceItem.setOnClickListener(clickListener);
                GlideApp.with(conferenceItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(conferenceItem.getPortraitImageView());

                // self
                if (profile.getUserId().equals(me.uid)) {
                    session.setupLocalVideoView(conferenceItem.videoContainer, scalingType);
                } else {
                    session.setupRemoteVideoView(userInfo.uid, profile.isScreenSharing(), conferenceItem.videoContainer, scalingType);
                }

                if (participantKey.equals(focusVideoUserId)) {
                    focusConferenceItem = conferenceItem;
                } else {
                    conferenceItem.setLayoutParams(new ViewGroup.LayoutParams(size / 3, size / 3));
                    participantGridView.addView(conferenceItem);
                }
            }
        }

        if (focusConferenceItem != null) {
            focusConferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            focusVideoContainerFrameLayout.addView(focusConferenceItem);
        }
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {
        String meUid = userViewModel.getUserId();
        VoipCallItem item = rootLinearLayout.findViewWithTag(meUid);
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
//        ((ConferenceActivity) getActivity()).showFloatingView(focusVideoUserId);
        // VoipBaseActivity#onStop会处理，这儿仅仅finish
        getActivity().finish();
    }

    @OnClick(R2.id.manageParticipantView)
    void addParticipant() {
        ((ConferenceActivity) getActivity()).showParticipantList();
    }

    @OnClick(R2.id.muteView)
    void mute() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            boolean toMute = !session.isAudioMuted();
            muteImageView.setSelected(toMute);
            session.muteAudio(toMute);
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
            boolean toMute = !session.videoMuted;
            videoImageView.setSelected(toMute);
            session.muteVideo(toMute);
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
            if (session.isAudience()) {
                return;
            }

//            shareScreenImageView.setSelected(!session.isScreenSharing());
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
            if (callSession != null && (callSession.getEndReason() == AVEngineKit.CallEndReason.RoomNotExist || callSession.getEndReason() == AVEngineKit.CallEndReason.RoomParticipantsFull)) {
                // do nothing
            } else {
                getActivity().finish();
            }
        }
    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();

        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }

        AVEngineKit.ParticipantProfile profile = session.getParticipantProfile(userId, screenSharing);
        if (profile == null || profile.isAudience()) {
            return;
        }

        String participantKey = VoipBaseActivity.participantKey(userId, screenSharing);
        if (participants.contains(participantKey) || participantKey.equals(focusVideoUserId)) {
            return;
        }

        manageParticipantTextView.setText("管理(" + (session.getParticipantProfiles().size() + 1) + ")");

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = Math.min(dm.widthPixels, dm.heightPixels);

        participantGridView.getLayoutParams().height = size;

        UserInfo userInfo = userViewModel.getUserInfo(userId, false);
        VoipCallItem conferenceItem = new VoipCallItem(getActivity());
        conferenceItem.setTag(participantKey);
        conferenceItem.getStatusTextView().setText(userInfo.displayName);
        conferenceItem.setOnClickListener(clickListener);
        GlideApp.with(conferenceItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(conferenceItem.getPortraitImageView());
        if (me.uid.equals(userId)) {
            session.setupLocalVideoView(conferenceItem.videoContainer, scalingType);
        } else {
            session.setupRemoteVideoView(userInfo.uid, screenSharing, conferenceItem.videoContainer, scalingType);
        }

        if (focusVideoUserId == null) {
            conferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            focusConferenceItem = conferenceItem;
            focusVideoUserId = participantKey;
            ((VoipBaseActivity) getActivity()).setFocusVideoUserId(focusVideoUserId);

            focusConferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            focusVideoContainerFrameLayout.addView(focusConferenceItem);
        } else {
            conferenceItem.setLayoutParams(new ViewGroup.LayoutParams(size / 3, size / 3));
            participantGridView.addView(conferenceItem);
        }
        participants.add(participantKey);
        startHideBarTimer();
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {
        if (userId.equals(ChatManager.Instance().getUserId()) && screenSharing) {
            return;
        }
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }

        removeParticipantView(VoipBaseActivity.participantKey(userId, screenSharing));
        if (!screenSharing) {
            Toast.makeText(getActivity(), ChatManager.Instance().getUserDisplayName(userId) + "离开了会议", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), ChatManager.Instance().getUserDisplayName(userId) + "结束了屏幕共享", Toast.LENGTH_SHORT).show();
        }

        manageParticipantTextView.setText("管理(" + (session.getParticipantProfiles().size() + 1) + ")");

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
    public void didChangeType(String userId, boolean audience, boolean screenSharing) {
        if (userId.equals(ChatManager.Instance().getUserId()) && screenSharing) {
            return;
        }
        String participantKey = VoipBaseActivity.participantKey(userId, screenSharing);
        if (audience) {
            removeParticipantView(participantKey);
            if (!screenSharing) {
                Toast.makeText(getActivity(), ChatManager.Instance().getUserDisplayName(userId) + "结束了互动", Toast.LENGTH_SHORT).show();
            }
        } else {
            didParticipantJoined(userId, screenSharing);
        }
        updateControlStatus();
    }

    @Override
    public void didMuteStateChanged(List<String> participants) {
        for (String participant : participants) {
            Log.e(TAG, "didMuteStateChanged" + participant);
            AVEngineKit.ParticipantProfile profile = AVEngineKit.Instance().getCurrentSession().getParticipantProfile(participant);
            VoipCallItem item = rootLinearLayout.findViewWithTag(participant);
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
            }
        }
    }

    @Override
    public void didCreateLocalVideoTrack() {
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        // 不能将头像等放在 surfaceView 的上面，然后在这儿隐藏头像，因为屏幕共享虽然收到流了，但可能还没有数据，如果通过这儿隐藏头像，会是透明的
        // 只能将头像等，放在 surfaceView 的下面
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
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

    private VoipCallItem getUserVoipCallItem(String userId) {
        return participantGridView.findViewWithTag(userId);
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        Log.d("jyj", userId + " volume " + volume);
        VoipCallItem multiCallItem = getUserVoipCallItem(VoipBaseActivity.participantKey(userId, false));
        if (multiCallItem != null) {
            if (volume > 1000) {
                multiCallItem.getStatusTextView().setVisibility(View.VISIBLE);
                multiCallItem.getStatusTextView().setText("正在说话");
            } else {
//                multiCallItem.getStatusTextView().setVisibility(View.GONE);
//                multiCallItem.getStatusTextView().setText("");
                multiCallItem.getStatusTextView().setVisibility(View.VISIBLE);
                multiCallItem.getStatusTextView().setText("正在说话");
            }
        }
    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {

    }

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String userId = (String) v.getTag();
            if (userId != null && !userId.equals(focusVideoUserId)) {
                VoipCallItem clickedConferenceItem = (VoipCallItem) v;
                int clickedIndex = participantGridView.indexOfChild(v);
                participantGridView.removeView(clickedConferenceItem);
                participantGridView.endViewTransition(clickedConferenceItem);

                clickedConferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                if (focusConferenceItem != null) {
                    focusVideoContainerFrameLayout.removeView(focusConferenceItem);
                    focusVideoContainerFrameLayout.endViewTransition(focusConferenceItem);
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    int size = Math.min(dm.widthPixels, dm.heightPixels);
                    participantGridView.addView(focusConferenceItem, clickedIndex, new FrameLayout.LayoutParams(size / 3, size / 3));
                }
                focusVideoContainerFrameLayout.addView(clickedConferenceItem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                focusConferenceItem = clickedConferenceItem;
                focusVideoUserId = userId;
                Log.e(TAG, "focusVideoUserId " + userId + " " + ChatManager.Instance().getUserId());
                ((VoipBaseActivity) getActivity()).setFocusVideoUserId(focusVideoUserId);

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
        if (bottomPanel.getVisibility() == View.GONE) {
            return;
        }
        handler.removeCallbacks(hideBarCallback);
        handler.postDelayed(hideBarCallback, 3000);
    }

    private final Runnable hideBarCallback = new Runnable() {
        @Override
        public void run() {
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
                bottomPanel.setVisibility(View.GONE);
                topBarView.setVisibility(View.GONE);
            }
        }
    };

    private final Handler handler = ChatManager.Instance().getMainHandler();

    private final Runnable updateCallDurationRunnable = new Runnable() {
        @Override
        public void run() {
            AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
            if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
                String text;
                if (session.getConnectedTime() == 0) {
                    text = "会议连接中";
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
            handler.postDelayed(updateCallDurationRunnable, 1000);
        }
    };

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
                    .onPositive((dialog1, which) -> changeMode(AVEngineKit.Instance().getCurrentSession().getCallId(), false))
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
        handler.removeCallbacks(hideBarCallback);
        handler.removeCallbacks(updateCallDurationRunnable);
    }
}
