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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import org.webrtc.StatsReport;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.glide.BlurTransformation;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class SingleAudioFragment extends Fragment implements AVEngineKit.CallSessionCallback {
    private AVEngineKit gEngineKit;

    ImageView backgroundImageView;

    ImageView portraitImageView;
    TextView nameTextView;
    ImageView muteImageView;
    ImageView spearImageView;
    ViewGroup incomingActionContainer;
    ViewGroup outgoingActionContainer;
    TextView descTextView;
    TextView durationTextView;

    private static final String TAG = "AudioFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_p2p_audio_layout, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.muteImageView).setOnClickListener(_v -> mute());
        view.findViewById(R.id.incomingHangupImageView).setOnClickListener(_v -> hangup());
        view.findViewById(R.id.outgoingHangupImageView).setOnClickListener(_v -> hangup());
        view.findViewById(R.id.acceptImageView).setOnClickListener(_v -> onCallConnect());
        view.findViewById(R.id.minimizeImageView).setOnClickListener(_v -> minimize());
        view.findViewById(R.id.speakerImageView).setOnClickListener(_v -> speakerClick());
    }

    private void bindViews(View view) {
        backgroundImageView = view.findViewById(R.id.backgroundImageView);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        nameTextView = view.findViewById(R.id.nameTextView);
        muteImageView = view.findViewById(R.id.muteImageView);
        spearImageView = view.findViewById(R.id.speakerImageView);
        incomingActionContainer = view.findViewById(R.id.incomingActionContainer);
        outgoingActionContainer = view.findViewById(R.id.outgoingActionContainer);
        descTextView = view.findViewById(R.id.descTextView);
        durationTextView = view.findViewById(R.id.durationTextView);
    }

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason reason) {
        // never called
    }

    @Override
    public void didChangeState(AVEngineKit.CallState state) {
        runOnUiThread(() -> {
            if (state == AVEngineKit.CallState.Connected) {
                incomingActionContainer.setVisibility(View.GONE);
                outgoingActionContainer.setVisibility(View.VISIBLE);
                descTextView.setVisibility(View.GONE);
                durationTextView.setVisibility(View.VISIBLE);
            } else if (state == AVEngineKit.CallState.Idle) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().finish();
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
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
        // never called
    }

    @Override
    public void didCreateLocalVideoTrack() {
        // never called
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {

    }

    @Override
    public void didRemoveRemoteVideoTrack(String s) {

    }

    @Override
    public void didError(String error) {

    }

    @Override
    public void didGetStats(StatsReport[] reports) {
        runOnUiThread(() -> {
            //hudFragment.updateEncoderStatistics(reports);
            // TODO
        });
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
        if (device == AVAudioManager.AudioDevice.WIRED_HEADSET || device == AVAudioManager.AudioDevice.EARPIECE || device == AVAudioManager.AudioDevice.BLUETOOTH) {
            spearImageView.setSelected(false);
        } else {
            spearImageView.setSelected(true);
        }
    }

    public void mute() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            boolean toMute = !session.isAudioMuted();
            session.muteAudio(toMute);
            muteImageView.setSelected(toMute);
        }
    }

    public void hangup() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null) {
            session.endCall();
        } else {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    public void onCallConnect() {
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

    public void minimize() {
        ((SingleCallActivity) getActivity()).showFloatingView(null);
    }

    public void speakerClick() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session == null || (session.getState() != AVEngineKit.CallState.Connected && session.getState() != AVEngineKit.CallState.Outgoing)) {
            return;
        }

        AVAudioManager audioManager = AVEngineKit.Instance().getAVAudioManager();
        AVAudioManager.AudioDevice currentAudioDevice = audioManager.getSelectedAudioDevice();
        if (currentAudioDevice == AVAudioManager.AudioDevice.WIRED_HEADSET || currentAudioDevice == AVAudioManager.AudioDevice.BLUETOOTH) {
            return;
        }
        if (currentAudioDevice == AVAudioManager.AudioDevice.SPEAKER_PHONE) {
            audioManager.selectAudioDevice(AVAudioManager.AudioDevice.EARPIECE);
            spearImageView.setSelected(false);
        } else {
            audioManager.selectAudioDevice(AVAudioManager.AudioDevice.SPEAKER_PHONE);
            spearImageView.setSelected(true);
        }
    }

    private void init() {
        gEngineKit = ((SingleCallActivity) getActivity()).getEngineKit();
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        if (session.getState() == AVEngineKit.CallState.Connected) {
            descTextView.setVisibility(View.GONE);
            outgoingActionContainer.setVisibility(View.VISIBLE);
            durationTextView.setVisibility(View.VISIBLE);
        } else {
            if (session.getState() == AVEngineKit.CallState.Outgoing) {
                descTextView.setText(R.string.av_waiting);
                outgoingActionContainer.setVisibility(View.VISIBLE);
                incomingActionContainer.setVisibility(View.GONE);
            } else {
                descTextView.setText(R.string.av_audio_invite);
                outgoingActionContainer.setVisibility(View.GONE);
                incomingActionContainer.setVisibility(View.VISIBLE);
            }
        }
        String targetId = session.getParticipantIds().get(0);
        UserInfo userInfo = ChatManager.Instance().getUserInfo(targetId, false);
        updateUserInfoViews(userInfo);

        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.userInfoLiveData().observe(getViewLifecycleOwner(), userInfos -> {
            for (UserInfo info : userInfos) {
                if (info.uid.equals(targetId)) {
                    updateUserInfoViews(info);
                    break;
                }
            }
        });
        muteImageView.setSelected(session.isAudioMuted());
        updateCallDuration();

        AVAudioManager audioManager = AVEngineKit.Instance().getAVAudioManager();
        spearImageView.setSelected(audioManager.getSelectedAudioDevice() == AVAudioManager.AudioDevice.SPEAKER_PHONE);
    }

    private void updateUserInfoViews(UserInfo userInfo) {
        Glide.with(this)
            .load(userInfo.portrait)
            .placeholder(R.mipmap.avatar_def)
            .apply(RequestOptions.bitmapTransform(new RoundedCorners(10)))
            .into(portraitImageView);
        Glide.with(this)
            .load(userInfo.portrait)
            .apply(RequestOptions.bitmapTransform(new BlurTransformation(10)))
            .into(backgroundImageView);
        nameTextView.setText(ChatManager.Instance().getUserDisplayName(userInfo));
    }

    private void runOnUiThread(Runnable runnable) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
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
