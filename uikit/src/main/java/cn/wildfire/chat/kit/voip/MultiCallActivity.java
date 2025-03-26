/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.group.PickGroupMemberActivity;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

public class MultiCallActivity extends VoipBaseActivity {

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 101;
    private String groupId;
    private AVEngineKit.CallSessionCallback currentCallSessionCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void init() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            finish();
            return;
        }
        groupId = session.getConversation().target;

        Fragment fragment;
        if (session.getState() == AVEngineKit.CallState.Incoming) {
            fragment = new MultiCallIncomingFragment();
        } else if (session.isAudioOnly()) {
            fragment = new MultiCallAudioFragment();
        } else {
            fragment = new MultiCallVideoFragment();
        }

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        }
        decorView.setSystemUiVisibility(uiOptions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        currentCallSessionCallback = (AVEngineKit.CallSessionCallback) fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commit();
    }


    // hangup 也会触发
    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        postAction(() -> {
            if (!isFinishing()) {
                finish();
            }
        });
    }

    // 自己的状态
    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
        postAction(() -> {
            currentCallSessionCallback.didChangeState(callState);
        });
    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        postAction(() -> {
            currentCallSessionCallback.didParticipantJoined(userId, screenSharing);
        });
    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {
        postAction(() -> {
            currentCallSessionCallback.didParticipantLeft(userId, callEndReason, screenSharing);
        });
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        postAction(() -> {
            currentCallSessionCallback.didReportAudioVolume(userId, volume);
        });
    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {
        postAction(() -> currentCallSessionCallback.didAudioDeviceChanged(device));
    }

    @Override
    public void didChangeMode(boolean audioOnly) {
        postAction(() -> {
            if (audioOnly) {
                Fragment fragment = new MultiCallAudioFragment();
                currentCallSessionCallback = (AVEngineKit.CallSessionCallback) fragment;
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit();
            } else {
                // never called
            }
        });
    }

    //@Override
    public void didChangeInitiator(String initiator) {

    }

    @Override
    public void didCreateLocalVideoTrack() {
        postAction(() -> {
            if (AVEngineKit.Instance().getCurrentSession() == null) {
                finish();
                return;
            }
            currentCallSessionCallback.didCreateLocalVideoTrack();
        });
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        postAction(() -> {
            currentCallSessionCallback.didReceiveRemoteVideoTrack(userId, screenSharing);
        });
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
        postAction(() -> {
            currentCallSessionCallback.didRemoveRemoteVideoTrack(userId);
        });
    }

    @Override
    public void didError(String reason) {
        postAction(() -> {
            currentCallSessionCallback.didError(reason);
        });
    }

    @Override
    public void didGetStats(StatsReport[] statsReports) {
        postAction(() -> {
            currentCallSessionCallback.didGetStats(statsReports);
        });
    }

    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            session.endCall();
        }
        finish();
    }

    void accept() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            finish();
            return;
        }

        Fragment fragment;
        if (session.isAudioOnly()) {
            fragment = new MultiCallAudioFragment();
        } else {
            fragment = new MultiCallVideoFragment();
        }
        currentCallSessionCallback = (AVEngineKit.CallSessionCallback) fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit();

        session.answerCall(session.isAudioOnly());
    }

    @Override
    public void didVideoMuted(String s, boolean b) {
        postAction(() -> currentCallSessionCallback.didVideoMuted(s, b));
    }

    void addParticipant(int maxNewInviteParticipantCount) {
        preventShowFloatingViewOnStop = true;
        Intent intent = new Intent(this, PickGroupMemberActivity.class);
        GroupViewModel groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        GroupInfo groupInfo = groupViewModel.getGroupInfo(groupId, false);
        intent.putExtra(PickGroupMemberActivity.GROUP_INFO, groupInfo);
        List<String> participants = getEngineKit().getCurrentSession().getParticipantIds();
        participants.add(ChatManager.Instance().getUserId());
        intent.putStringArrayListExtra(PickGroupMemberActivity.CHECKED_MEMBER_IDS, (ArrayList<String>) participants);
        intent.putStringArrayListExtra(PickGroupMemberActivity.UNCHECKABLE_MEMBER_IDS, (ArrayList<String>) participants);
        intent.putExtra(PickGroupMemberActivity.MAX_COUNT, maxNewInviteParticipantCount);
        startActivityForResult(intent, REQUEST_CODE_ADD_PARTICIPANT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT) {
            preventShowFloatingViewOnStop = false;
            if (resultCode == RESULT_OK) {
                List<String> newParticipants = data.getStringArrayListExtra(PickGroupMemberActivity.EXTRA_RESULT);
                if (newParticipants != null && !newParticipants.isEmpty()) {
                    AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
                    session.inviteNewParticipants(newParticipants);
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
