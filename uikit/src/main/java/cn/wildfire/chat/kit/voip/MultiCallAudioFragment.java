/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;

import org.webrtc.StatsReport;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class MultiCallAudioFragment extends Fragment implements AVEngineKit.CallSessionCallback {
    TextView durationTextView;
    GridLayout audioContainerGridLayout;
    ImageView speakerImageView;
    ImageView muteImageView;

    private List<String> participants;
    private UserInfo me;
    private UserViewModel userViewModel;

    public static final String TAG = "MultiCallVideoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_audio_outgoing_connected, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.minimizeImageView).setOnClickListener(_v -> minimize());
        view.findViewById(R.id.addParticipantImageView).setOnClickListener(_v -> addParticipant());
        view.findViewById(R.id.muteImageView).setOnClickListener(_v -> mute());
        view.findViewById(R.id.speakerImageView).setOnClickListener(_v -> speaker());
        view.findViewById(R.id.hangupImageView).setOnClickListener(_v -> hangup());
    }

    private void bindViews(View view) {
        durationTextView = view.findViewById(R.id.durationTextView);
        audioContainerGridLayout = view.findViewById(R.id.audioContainerGridLayout);
        speakerImageView = view.findViewById(R.id.speakerImageView);
        muteImageView = view.findViewById(R.id.muteImageView);
    }

    private void init() {
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null) {
            getActivity().finish();
            return;
        }

        muteImageView.setSelected(session.isAudioMuted());

        initParticipantsView(session);
        updateParticipantStatus(session);
        updateCallDuration();

        AVAudioManager  audioManager = AVEngineKit.Instance().getAVAudioManager();
        speakerImageView.setSelected(audioManager.getSelectedAudioDevice() == AVAudioManager.AudioDevice.SPEAKER_PHONE);
    }

    private void initParticipantsView(AVEngineKit.CallSession session) {

        me = userViewModel.getUserInfo(userViewModel.getUserId(), false);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        audioContainerGridLayout.getLayoutParams().height = with;
        audioContainerGridLayout.removeAllViews();

        // session里面的participants包含除自己外的所有人
        participants = session.getParticipantIds();
        List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participants);
        participantUserInfos.add(me);
        int size = with / Math.max((int) Math.ceil(Math.sqrt(participantUserInfos.size())), 3);
        for (UserInfo userInfo : participantUserInfos) {
            VoipCallItem voipCallItem = new VoipCallItem(getActivity());
            voipCallItem.setTag(userInfo.uid);

            voipCallItem.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            voipCallItem.getStatusTextView().setText(R.string.connecting);
            Glide.with(voipCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(voipCallItem.getPortraitImageView());
            audioContainerGridLayout.addView(voipCallItem);
        }
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {
        int count = audioContainerGridLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = audioContainerGridLayout.getChildAt(i);
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

    void minimize() {
        ((MultiCallActivity) getActivity()).showFloatingView(null);
    }

    void addParticipant() {
        ((MultiCallActivity) getActivity()).addParticipant(AVEngineKit.MAX_AUDIO_PARTICIPANT_COUNT - participants.size() - 1);
    }

    void mute() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            boolean toMute = !session.isAudioMuted();
            session.muteAudio(toMute);
            muteImageView.setSelected(toMute);
        }
    }

    void speaker() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            AVAudioManager audioManager = AVEngineKit.Instance().getAVAudioManager();
            AVAudioManager.AudioDevice currentAudioDevice = audioManager.getSelectedAudioDevice();
            if(currentAudioDevice == AVAudioManager.AudioDevice.WIRED_HEADSET ||currentAudioDevice == AVAudioManager.AudioDevice.BLUETOOTH){
                return;
            }
            if(currentAudioDevice == AVAudioManager.AudioDevice.SPEAKER_PHONE){
                audioManager.selectAudioDevice(AVAudioManager.AudioDevice.EARPIECE);
                speakerImageView.setSelected(false);
            }else {
                audioManager.selectAudioDevice(AVAudioManager.AudioDevice.SPEAKER_PHONE);
                speakerImageView.setSelected(true);
            }
        }
    }

    void hangup() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null) {
            session.endCall();
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
        if (callSession != null && callState == AVEngineKit.CallState.Connected) {
            updateParticipantStatus(callSession);
        } else if (callState == AVEngineKit.CallState.Idle) {
            if (getActivity() == null) {
                return;
            }
            getActivity().finish();
        }
    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        if (participants.contains(userId)) {
            return;
        }
        int count = audioContainerGridLayout.getChildCount();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;
        for (int i = 0; i < count; i++) {
            View view = audioContainerGridLayout.getChildAt(i);
            // 将自己放到最后
            if (me.uid.equals(view.getTag())) {

                UserInfo info = userViewModel.getUserInfo(userId, false);
                VoipCallItem voipCallItem = new VoipCallItem(getActivity());
                voipCallItem.setTag(info.uid);

                voipCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));

                voipCallItem.getStatusTextView().setText(R.string.connecting);
                Glide.with(voipCallItem).load(info.portrait).placeholder(R.mipmap.avatar_def).into(voipCallItem.getPortraitImageView());
                audioContainerGridLayout.addView(voipCallItem, i);
                break;
            }
        }
        participants.add(userId);
    }

    private VoipCallItem getUserVoipCallItem(String userId) {
        return audioContainerGridLayout.findViewWithTag(userId);
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {
        View view = audioContainerGridLayout.findViewWithTag(userId);
        if (view != null) {
            audioContainerGridLayout.removeView(view);
        }
        participants.remove(userId);

        Toast.makeText(getActivity(), getString(R.string.participant_left_call, ChatManager.Instance().getUserDisplayName(userId)), Toast.LENGTH_SHORT).show();
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
        VoipCallItem voipCallItem = getUserVoipCallItem(userId);
        if (voipCallItem != null) {
            if (volume > 1000) {
                voipCallItem.getStatusTextView().setVisibility(View.VISIBLE);
                voipCallItem.getStatusTextView().setText(R.string.speaking);
            } else {
                voipCallItem.getStatusTextView().setVisibility(View.GONE);
                voipCallItem.getStatusTextView().setText("");
            }
        }
    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {
        if(device == AVAudioManager.AudioDevice.WIRED_HEADSET || device == AVAudioManager.AudioDevice.EARPIECE || device == AVAudioManager.AudioDevice.BLUETOOTH) {
            speakerImageView.setSelected(false);
        } else {
            speakerImageView.setSelected(true);
        }
    }

    private Handler handler = new Handler();

    private void updateCallDuration() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
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
