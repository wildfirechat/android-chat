package cn.wildfire.chat.kit.voip;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
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

import org.webrtc.StatsReport;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class MultiCallAudioFragment extends Fragment implements AVEngineKit.CallSessionCallback {
    @BindView(R.id.durationTextView)
    TextView durationTextView;
    @BindView(R.id.audioContainerGridLayout)
    GridLayout audioContainerGridLayout;
    @BindView(R.id.speakerImageView)
    ImageView speakerImageView;
    @BindView(R.id.muteImageView)
    ImageView muteImageView;

    private List<String> participants;
    private UserInfo me;
    private UserViewModel userViewModel;
    private boolean isSpeakerOn;
    private boolean micEnabled = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_audio_outgoing_connected, container, false);
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

        initParticipantsView(session);
        updateParticipantStatus(session);
        updateCallDuration();
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
        for (UserInfo userInfo : participantUserInfos) {
            MultiCallItem multiCallItem = new MultiCallItem(getActivity());
            multiCallItem.setTag(userInfo.uid);

            multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));
            multiCallItem.getStatusTextView().setText(R.string.connecting);
            GlideApp.with(multiCallItem).load(userInfo.portrait).into(multiCallItem.getPortraitImageView());
            audioContainerGridLayout.addView(multiCallItem);
        }
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {
        int count = audioContainerGridLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = audioContainerGridLayout.getChildAt(i);
            String userId = (String) view.getTag();
            if (me.uid.equals(userId)) {
                ((MultiCallItem) view).getStatusTextView().setVisibility(View.GONE);
            } else {
                PeerConnectionClient client = session.getClient(userId);
                if (client.state == AVEngineKit.CallState.Connected) {
                    ((MultiCallItem) view).getStatusTextView().setVisibility(View.GONE);
                }
            }
        }
    }

    @OnClick(R.id.minimizeImageView)
    void minimize() {
        ((MultiCallActivity) getActivity()).showFloatingView();
    }

    @OnClick(R.id.addParticipantImageView)
    void addParticipant() {
        ((MultiCallActivity) getActivity()).addParticipant();
    }

    @OnClick(R.id.muteImageView)
    void mute() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            if (session.muteAudio(!micEnabled)) {
                micEnabled = !micEnabled;
            }
            muteImageView.setSelected(!micEnabled);
        }
    }

    @OnClick(R.id.speakerImageView)
    void speaker() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        isSpeakerOn = !isSpeakerOn;
        audioManager.setMode(isSpeakerOn ? AudioManager.MODE_NORMAL : AudioManager.MODE_IN_COMMUNICATION);
        speakerImageView.setSelected(isSpeakerOn);
        audioManager.setSpeakerphoneOn(isSpeakerOn);
    }

    @OnClick(R.id.hangupImageView)
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
        AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
        if (callState == AVEngineKit.CallState.Connected) {
            updateParticipantStatus(callSession);
        } else if (callState == AVEngineKit.CallState.Idle) {
            getActivity().finish();
        }
        Toast.makeText(getActivity(), "" + callState.name(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didParticipantJoined(String userId) {
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
                MultiCallItem multiCallItem = new MultiCallItem(getActivity());
                multiCallItem.setTag(info.uid);

                multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));

                multiCallItem.getStatusTextView().setText(R.string.connecting);
                GlideApp.with(multiCallItem).load(info.portrait).into(multiCallItem.getPortraitImageView());
                audioContainerGridLayout.addView(multiCallItem, i);
                break;
            }
        }
        participants.add(userId);
    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason) {
        View view = audioContainerGridLayout.findViewWithTag(userId);
        if (view != null) {
            audioContainerGridLayout.removeView(view);
        }
        participants.remove(userId);

        Toast.makeText(getActivity(), "用户" + ChatManager.Instance().getUserDisplayName(userId) + "离开了通话", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didChangeMode(boolean audioOnly) {

    }

    @Override
    public void didCreateLocalVideoTrack() {

    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId) {

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
