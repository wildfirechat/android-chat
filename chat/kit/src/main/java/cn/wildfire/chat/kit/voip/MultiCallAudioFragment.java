package cn.wildfire.chat.kit.voip;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private List<String> participants;
    private UserInfo me;
    private UserViewModel userViewModel;

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

        updateParticipantStatus(session);
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {

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
            multiCallItem.getStatusTextView().setText(userInfo.displayName);
            GlideApp.with(multiCallItem).load(userInfo.portrait).into(multiCallItem.getPortraitImageView());

            if (!me.uid.equals(userInfo.uid)) {
                PeerConnectionClient client = session.getClient(userInfo.uid);
                if (client.state == AVEngineKit.CallState.Connected) {
                    multiCallItem.getStatusTextView().setVisibility(View.GONE);
                }
            }
            audioContainerGridLayout.addView(multiCallItem);
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

    }

    @OnClick(R.id.speakerImageView)
    void speaker() {

    }

    @OnClick(R.id.videoImageView)
    void video() {

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

    // 自己的状态
    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
        if (callState == AVEngineKit.CallState.Connected) {
            for (String participant : participants) {
                PeerConnectionClient client = AVEngineKit.Instance().getCurrentSession().getClient(participant);
                // 自己时，是空的
                if (client != null) {
                    if (client.state == AVEngineKit.CallState.Connected) {
                        View view = audioContainerGridLayout.findViewWithTag(participant);
                        ((MultiCallItem) view).getStatusTextView().setVisibility(View.GONE);
                    }
                }
            }
        } else if (callState == AVEngineKit.CallState.Idle) {
            getActivity().finish();
        }
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

                multiCallItem.getStatusTextView().setText(info.displayName);
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
}
