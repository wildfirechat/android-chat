/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.afollestad.materialdialogs.MaterialDialog;

import org.webrtc.StatsReport;

import java.util.List;

import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceActivity extends VoipBaseActivity {

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 102;
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

        Fragment fragment;
        if (session.isAudioOnly()) {
            fragment = new ConferenceAudioFragment();
        } else {
            fragment = new ConferenceVideoFragment();
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
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        String callId = session.getCallId();
        boolean audioOnly = session.isAudioOnly();
        String pin = session.getPin();
        String title = session.getTitle();
        String desc = session.getDesc();
        boolean audience = session.isAudience();
        String host = session.getHost();
        boolean advanced = session.isAdvanced();

        postAction(() -> {
            if (callEndReason == AVEngineKit.CallEndReason.RoomNotExist) {
                //Todo 检查用户是不是host，如果是host提示用户是不是重新开启会议；如果不是host，提醒用户联系host开启会议。
                String selfUid = ChatManager.Instance().getUserId();
                if (selfUid.equals(host)) {
                    new MaterialDialog.Builder(this)
                        .content("会议已结束，是否重新开启会议")
                        .negativeText("否")
                        .positiveText("是")
                        .onPositive((dialog, which) -> {
                            finish();
                            new Handler().postDelayed(() -> {
                                AVEngineKit.CallSession newSession = AVEngineKit.Instance().startConference(callId, audioOnly, pin, host, title, desc, audience, advanced, false, this);
                                if (newSession == null) {
                                    Toast.makeText(this, "创建会议失败", Toast.LENGTH_SHORT).show();
                                } else {
                                    Intent intent = new Intent(getApplicationContext(), ConferenceActivity.class);
                                    startActivity(intent);
                                }
                            }, 800);
                        })
                        .onNegative((dialog, which) -> finish())
                        .show();
                } else {
                    Toast.makeText(this, "请联系主持人开启会议", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else if (callEndReason == AVEngineKit.CallEndReason.RoomParticipantsFull) {
                AVEngineKit.CallSession newSession = AVEngineKit.Instance().joinConference(callId, audioOnly, pin, host, title, desc, audience, advanced, false, false, this);
                if (newSession == null) {
                    Toast.makeText(this, "加入会议失败", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    newSession.setCallback(ConferenceActivity.this);
                }
            } else if (!isFinishing()) {
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
                Fragment fragment = new ConferenceAudioFragment();
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

    @Override
    public void didChangeType(String userId, boolean audience, boolean screenSharing) {
        postAction(() -> {
            currentCallSessionCallback.didChangeType(userId, audience, screenSharing);
        });
    }

    // multi call
    @Override
    public void didVideoMuted(String s, boolean b) {
        postAction(() -> currentCallSessionCallback.didVideoMuted(s, b));
    }

    @Override
    public void didMuteStateChanged(List<String> participants) {
        postAction(() -> currentCallSessionCallback.didMuteStateChanged(participants));
    }

    void showParticipantList() {
        isInvitingNewParticipant = true;
        Intent intent = new Intent(this, ConferenceParticipantListActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ADD_PARTICIPANT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT) {
            isInvitingNewParticipant = false;
        }
    }
}
