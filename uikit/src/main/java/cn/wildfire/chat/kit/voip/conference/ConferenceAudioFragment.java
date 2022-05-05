/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

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
import cn.wildfire.chat.kit.voip.VoipCallItem;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceAudioFragment extends BaseConferenceFragment implements AVEngineKit.CallSessionCallback {
    @BindView(R2.id.durationTextView)
    TextView durationTextView;

    @BindView(R2.id.manageParticipantTextView)
    TextView manageParticipantTextView;

    @BindView(R2.id.audioContainerGridLayout)
    GridLayout participantGridView;

    @BindView(R2.id.focusAudioContainerFrameLayout)
    FrameLayout focusAudioContainerFrameLayout;

    @BindView(R2.id.speakerImageView)
    ImageView speakerImageView;
    @BindView(R2.id.muteImageView)
    ImageView audioImageView;

    private List<String> participants;
    private UserInfo me;
    private UserViewModel userViewModel;

    public static final String TAG = "ConferenceVideoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference_audio_connected, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null) {
            getActivity().finish();
            return;
        }

        audioImageView.setSelected(session.isAudioMuted());

        initParticipantsView(session);
        updateParticipantStatus(session);
        updateCallDuration();

        AVAudioManager audioManager = AVEngineKit.Instance().getAVAudioManager();
        speakerImageView.setSelected(audioManager.getSelectedAudioDevice() == AVAudioManager.AudioDevice.SPEAKER_PHONE);

        manageParticipantTextView.setText("管理(" + (session.getParticipantIds().size() + 1) + ")");
    }

    private void initParticipantsView(AVEngineKit.CallSession session) {

        me = userViewModel.getUserInfo(userViewModel.getUserId(), false);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        participantGridView.getLayoutParams().height = with;
        participantGridView.removeAllViews();

        // session里面的participants包含除自己外的所有人
        participants = new ArrayList();
        List<AVEngineKit.ParticipantProfile> profiles = session.getParticipantProfiles();
        if (profiles != null) {
            profiles = profiles.stream().filter(p -> !p.isAudience()).collect(Collectors.toList());
            for (AVEngineKit.ParticipantProfile profile : profiles) {
                participants.add(profile.getUserId());
            }
        }

        List<UserInfo> participantUserInfos;
        if (participants != null && !participants.isEmpty()) {
            participantUserInfos = userViewModel.getUserInfos(participants);
        } else {
            participantUserInfos = new ArrayList<>();
        }
        AVEngineKit.ParticipantProfile myProfile = session.getMyProfile();
        if (!myProfile.isAudience()) {
            participantUserInfos.add(me);
        }

        int size = with / Math.max((int) Math.ceil(Math.sqrt(participantUserInfos.size())), 3);
        for (UserInfo userInfo : participantUserInfos) {
            VoipCallItem multiCallItem = new VoipCallItem(getActivity());
            multiCallItem.setTag(userInfo.uid);

            multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            multiCallItem.getStatusTextView().setText(R.string.connecting);
            GlideApp.with(multiCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
            participantGridView.addView(multiCallItem);
        }
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {
        int count = participantGridView.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = participantGridView.getChildAt(i);
            String userId = (String) view.getTag();
            if (me.uid.equals(userId)) {
                ((VoipCallItem) view).getStatusTextView().setVisibility(View.GONE);
            } else {
                PeerConnectionClient client = session.getClient(userId);
                if (client.state == AVEngineKit.CallState.Connected) {
                    ((VoipCallItem) view).getStatusTextView().setVisibility(View.GONE);
                }
            }
        }
    }

    @OnClick(R2.id.minimizeImageView)
    void minimize() {
        ((ConferenceActivity) getActivity()).showFloatingView(null);
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
            session.muteAudio(toMute);
            audioImageView.setSelected(toMute);
        }
    }

    @OnClick(R2.id.speakerView)
    void speaker() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            AVAudioManager audioManager = AVEngineKit.Instance().getAVAudioManager();
            AVAudioManager.AudioDevice currentAudioDevice = audioManager.getSelectedAudioDevice();
            if (currentAudioDevice == AVAudioManager.AudioDevice.WIRED_HEADSET || currentAudioDevice == AVAudioManager.AudioDevice.BLUETOOTH) {
                return;
            }
            if (currentAudioDevice == AVAudioManager.AudioDevice.SPEAKER_PHONE) {
                audioManager.selectAudioDevice(AVAudioManager.AudioDevice.EARPIECE);
                speakerImageView.setSelected(false);
            } else {
                audioManager.selectAudioDevice(AVAudioManager.AudioDevice.SPEAKER_PHONE);
                speakerImageView.setSelected(true);
            }
        }
    }

    @OnClick(R2.id.hangupView)
    void hangup() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null) {
            if (ChatManager.Instance().getUserId().equals(session.getHost())) {
                new AlertDialog.Builder(getActivity())
                    .setMessage("请选择是否结束会议")
                    .setIcon(R.mipmap.ic_launcher)
                    .setNeutralButton("退出会议", (dialogInterface, i) -> {
                        if (session.getState() != AVEngineKit.CallState.Idle) session.leaveConference(false);
                    })
                    .setPositiveButton("结束会议", (dialogInterface, i) -> {
                        if (session.getState() != AVEngineKit.CallState.Idle) session.leaveConference(true);
                    })
                    .create()
                    .show();
            } else {
                session.leaveConference(false);
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
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
        if (callState == AVEngineKit.CallState.Connected) {
            updateParticipantStatus(callSession);
        } else if (callState == AVEngineKit.CallState.Idle) {
            getActivity().finish();
        }
    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        if (participants.contains(userId)) {
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

        int count = participantGridView.getChildCount();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;
        UserInfo info = userViewModel.getUserInfo(userId, false);
        VoipCallItem multiCallItem = new VoipCallItem(getActivity());
        multiCallItem.setTag(info.uid);

        multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));

        multiCallItem.getStatusTextView().setText(R.string.connecting);
        GlideApp.with(multiCallItem).load(info.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
        int pos = 0;
        for (int i = 0; i < count; i++) {
            View view = participantGridView.getChildAt(i);
            // 将自己放到最后
            if (me.uid.equals(view.getTag())) {
                pos = i;
                break;
            }
        }
        participants.add(userId);
        participantGridView.addView(multiCallItem, pos);
    }

    private VoipCallItem getUserVoipCallItem(String userId) {
        return participantGridView.findViewWithTag(userId);
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {
        View view = participantGridView.findViewWithTag(userId);
        if (view != null) {
            participantGridView.removeView(view);
        }
        participants.remove(userId);

        Toast.makeText(getActivity(), "用户" + ChatManager.Instance().getUserDisplayName(userId) + "离开了通话", Toast.LENGTH_SHORT).show();

        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }
        manageParticipantTextView.setText("管理(" + (session.getParticipantIds().size() + 1) + ")");
    }

    @Override
    public void didChangeMode(boolean audioOnly) {

    }

    @Override
    public void didCreateLocalVideoTrack() {

    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {

    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {

    }

    @Override
    public void didError(String s) {
        Toast.makeText(getActivity(), "发生错误" + s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didGetStats(StatsReport[] statsReports) {

    }

    @Override
    public void didVideoMuted(String s, boolean b) {

    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        Log.d(TAG, userId + " volume " + volume);
        VoipCallItem multiCallItem = getUserVoipCallItem(userId);
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
        if (device == AVAudioManager.AudioDevice.WIRED_HEADSET || device == AVAudioManager.AudioDevice.EARPIECE || device == AVAudioManager.AudioDevice.BLUETOOTH) {
            speakerImageView.setSelected(false);
        } else {
            speakerImageView.setSelected(true);
        }
    }

    @Override
    public void didChangeType(String userId, boolean audience, boolean screenSharing) {
        if (!audience) {
            didParticipantJoined(userId, screenSharing);
        } else {
            participants.remove(userId);
            int count = participantGridView.getChildCount();
            for (int i = 0; i < count; i++) {
                View view = participantGridView.getChildAt(i);
                if (userId.equals(view.getTag())) {
                    participantGridView.removeView(view);
                    break;
                }
            }
        }
    }

    public void didMuteStateChanged(List<String> participants) {

    }

    private Handler handler = new Handler();

    private void updateCallDuration() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
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
}
