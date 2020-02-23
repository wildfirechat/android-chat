package cn.wildfire.chat.kit.voip;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;

public class MultiCallIncomingActivity extends VoipBaseActivity {

    @BindView(R.id.invitorImageView)
    ImageView invitorImageView;
    @BindView(R.id.invitorTextView)
    TextView invitorTextView;
    @BindView(R.id.participantGridView)
    RecyclerView participantRecyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.av_multi_incoming);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session == null) {
            finish();
            return;
        }
        session.setCallback(this);
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        UserInfo invitor = userViewModel.getUserInfo(session.initiator, false);
        invitorTextView.setText(invitor.displayName);
        GlideApp.with(this).load(invitor.portrait).into(invitorImageView);

        List<String> participants = session.getParticipantIds();
        participants.remove(invitor.uid);
        List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participants);

        FlexboxLayoutManager manager = new FlexboxLayoutManager(this, FlexDirection.ROW);
        manager.setJustifyContent(JustifyContent.CENTER);

        MultiCallParticipantAdapter adapter = new MultiCallParticipantAdapter();
        adapter.setParticipants(participantUserInfos);
        participantRecyclerView.setLayoutManager(manager);
        participantRecyclerView.setAdapter(adapter);
    }


    @OnClick(R.id.hangupImageView)
    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            session.endCall();
        }
        finish();
    }

    @OnClick(R.id.acceptImageView)
    void accept() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        session.answerCall(session.isAudioOnly());
        Intent intent = new Intent(this, MultiCallActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        finish();
    }
}
