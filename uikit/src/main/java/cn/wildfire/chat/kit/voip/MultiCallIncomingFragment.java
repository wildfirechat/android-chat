/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import org.webrtc.StatsReport;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class MultiCallIncomingFragment extends Fragment implements AVEngineKit.CallSessionCallback {

    ImageView invitorImageView;
    TextView invitorTextView;
    RecyclerView participantRecyclerView;

    ImageView acceptImageView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_incoming, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.hangupImageView).setOnClickListener(_v -> hangup());
        view.findViewById(R.id.acceptImageView).setOnClickListener(_v -> accept());
    }

    private void bindViews(View view) {
        invitorImageView = view.findViewById(R.id.invitorImageView);
        invitorTextView = view.findViewById(R.id.invitorTextView);
        participantRecyclerView = view.findViewById(R.id.participantGridView);
        acceptImageView = view.findViewById(R.id.acceptImageView);
    }

    private void init() {

        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            return;
        }
        if(session.isAudioOnly()) {
            acceptImageView.setImageResource(R.drawable.av_voice_answer_selector);
        }
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        UserInfo invitor = userViewModel.getUserInfo(session.initiator, false);
        invitorTextView.setText(invitor.displayName);
        Glide.with(this).load(invitor.portrait).placeholder(R.mipmap.avatar_def).into(invitorImageView);

        List<String> participants = session.getParticipantIds();
        participants.remove(invitor.uid);

        //把自己也加入到用户列表中
        participants.add(ChatManager.Instance().getUserId());
        List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participants);

        FlexboxLayoutManager manager = new FlexboxLayoutManager(getActivity(), FlexDirection.ROW);
        manager.setJustifyContent(JustifyContent.CENTER);

        MultiCallParticipantAdapter adapter = new MultiCallParticipantAdapter();
        adapter.setParticipants(participantUserInfos);
        participantRecyclerView.setLayoutManager(manager);
        participantRecyclerView.setAdapter(adapter);
    }


    void hangup() {
        ((MultiCallActivity) getActivity()).hangup();
    }

    void accept() {
        ((MultiCallActivity) getActivity()).accept();
    }

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason reason) {
        getActivity().finish();
    }

    @Override
    public void didChangeState(AVEngineKit.CallState state) {

    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        List<UserInfo> participants = ((MultiCallParticipantAdapter)participantRecyclerView.getAdapter()).getParticipants();
        boolean exist = false;
        for (UserInfo user :
                participants) {
            if (user.uid.equals(userId)) {
                exist = true;
                break;
            }
        }
        if (!exist) {
            UserViewModel userViewModel =WfcUIKit.getAppScopeViewModel(UserViewModel.class);
            participants.add(userViewModel.getUserInfo(userId, false));
            participantRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason reason, boolean screenSharing) {
        List<UserInfo> participants = ((MultiCallParticipantAdapter)participantRecyclerView.getAdapter()).getParticipants();
        for (UserInfo user :
                participants) {
            if (user.uid.equals(userId)) {
                participants.remove(user);
                participantRecyclerView.getAdapter().notifyDataSetChanged();
                break;
            }
        }
        if (AVEngineKit.Instance().getCurrentSession()!= null && AVEngineKit.Instance().getCurrentSession().getInitiator() == null) {
            invitorTextView.setText("");
            invitorImageView.setImageBitmap(null);
        }
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
    public void didError(String error) {

    }

    @Override
    public void didGetStats(StatsReport[] reports) {

    }

    @Override
    public void didVideoMuted(String userId, boolean videoMuted) {

    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {

    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {

    }
}
