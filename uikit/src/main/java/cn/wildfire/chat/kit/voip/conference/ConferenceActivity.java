/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;

import com.afollestad.materialdialogs.MaterialDialog;

import org.webrtc.StatsReport;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfire.chat.kit.widget.AlertDialogActivity;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.ConferenceInviteMessageContent;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceActivity extends VoipBaseActivity {

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 102;
    private AVEngineKit.CallSessionCallback currentCallSessionCallback;

    private Observer<Object> onRequestMuteObserver;
    private Observer<Object> onCancelMuteAllObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.av_conference_activity);
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveDataBus.unsubscribe("onRequestMute", onRequestMuteObserver);
        LiveDataBus.unsubscribe("onCancelMuteAll", onCancelMuteAllObserver);
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
        fragment = new ConferenceFragment();
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
            .add(R.id.mainLayoutContainer, fragment)
            .add(R.id.messageLayoutContainer, new ConferenceMessageFragment())
            .commit();

        onRequestMuteObserver = o -> {
            ConferenceManager manager = ConferenceManager.getManager();
            Pair<Boolean, Boolean> value = (Pair<Boolean, Boolean>) o;
            boolean audio = value.first;
            boolean mute = value.second;
            if (!mute) {
                preventShowFloatingViewOnStop = true;
                AlertDialogActivity.showAlterDialog(this, audio ? getString(R.string.host_invite_speak) : getString(R.string.host_invite_camera), false, getString(R.string.reject), getString(R.string.accept),
                    () -> {
                        preventShowFloatingViewOnStop = false;
                        Toast.makeText(this, audio ? getString(R.string.rejected_speak_invite) : getString(R.string.rejected_camera_invite), Toast.LENGTH_SHORT).show();
                    },
                    () -> {
                        preventShowFloatingViewOnStop = false;
                        Toast.makeText(this, audio ? getString(R.string.accepted_speak_invite) : getString(R.string.accepted_camera_invite), Toast.LENGTH_SHORT).show();
                        if (audio) {
                            manager.muteAudio(false);
                        } else {
                            manager.muteVideo(false);
                        }
                    });
            } else {
                Toast.makeText(this, getString(R.string.host_closed_your_speech), Toast.LENGTH_SHORT).show();
                if (audio) {
                    manager.muteAudio(true);
                } else {
                    manager.muteVideo(true);
                }
            }
        };
        LiveDataBus.subscribeForever("onRequestMute", onRequestMuteObserver);
        onCancelMuteAllObserver = o -> {
            Pair<Boolean, Boolean> value = (Pair<Boolean, Boolean>) o;
            boolean audio = value.first;
            boolean requestUnmute = value.second;
            ConferenceManager manager = ConferenceManager.getManager();
            if (requestUnmute) {
                preventShowFloatingViewOnStop = true;
                AlertDialogActivity.showAlterDialog(this, audio ? getString(R.string.host_unmuted_all_mic) : getString(R.string.host_unmuted_all_camera), false, getString(R.string.ignore), getString(R.string.open),
                    () -> {
                        preventShowFloatingViewOnStop = false;
                    },
                    () -> {
                        preventShowFloatingViewOnStop = false;
                        if (audio) {
                            manager.muteAudio(false);
                        } else {
                            manager.muteVideo(false);
                        }
                    });
            }
            Toast.makeText(this, getString(R.string.host_muted_all), Toast.LENGTH_SHORT).show();
        };
        LiveDataBus.subscribe("onCancelMuteAll", this, onCancelMuteAllObserver);
    }

    public void showKeyboardDialogFragment() {
        KeyboardDialogFragment keyboardDialogFragment = new ConferenceMessageInputDialogFragment();
        keyboardDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullScreen_Transparent);
        keyboardDialogFragment.show(getSupportFragmentManager(), "keyboardDialog");
    }

    // hangup 也会触发
    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        // 主动挂断
        ConferenceInfo conferenceInfo = ConferenceManager.getManager().getCurrentConferenceInfo();
        if (conferenceInfo == null) {
            finish();
            return;
        }
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        String callId = session.getCallId();
        boolean audioOnly = session.isAudioOnly();
        String pin = session.getPin();
        String title = session.getTitle();
        String desc = session.getDesc();
        boolean audience = session.isAudience();
        String host = conferenceInfo.getOwner();
        boolean advanced = session.isAdvanced();

        postAction(() -> {
            if (callEndReason == AVEngineKit.CallEndReason.RoomNotExist) {
                //Todo 检查用户是不是host，如果是host提示用户是不是重新开启会议；如果不是host，提醒用户联系host开启会议。
                String selfUid = ChatManager.Instance().getUserId();
                if (selfUid.equals(host)) {
                    new MaterialDialog.Builder(this)
                        .content(R.string.conf_ended_restart_prompt)
                        .negativeText(R.string.no)
                        .positiveText(R.string.yes)
                        .onPositive((dialog, which) -> {
                            finish();
                            new Handler().postDelayed(() -> {
                                AVEngineKit.CallSession newSession = AVEngineKit.Instance().startConference(callId, audioOnly, pin, host, title, desc, audience, advanced, false, this);
                                if (newSession == null) {
                                    Toast.makeText(this, R.string.create_conf_failed, Toast.LENGTH_SHORT).show();
                                } else {
                                    Intent intent = new Intent(getApplicationContext(), ConferenceActivity.class);
                                    startActivity(intent);
                                }
                            }, 800);
                        })
                        .onNegative((dialog, which) -> finish())
                        .show();
                } else {
                    Toast.makeText(this, R.string.contact_host_start_conf, Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else if (callEndReason == AVEngineKit.CallEndReason.RoomParticipantsFull) {
                new MaterialDialog.Builder(this)
                    .content(R.string.join_as_audience_prompt)
                    .negativeText(R.string.no)
                    .positiveText(R.string.yes)
                    .onPositive((dialog, which) -> {
                        finish();
                        new Handler().postDelayed(() -> {
                            AVEngineKit.CallSession newSession = AVEngineKit.Instance().joinConference(callId, audioOnly, pin, host, title, desc, true, advanced, false, false, this);
                            if (newSession == null) {
                                Toast.makeText(this, R.string.join_conf_failed, Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Intent intent = new Intent(getApplicationContext(), ConferenceActivity.class);
                                startActivity(intent);
                            }
                        }, 800);
                    })
                    .onNegative((dialog, which) -> finish())
                    .show();
            } else if (!isFinishing()) {
                ConferenceManager.getManager().addHistory(conferenceInfo, System.currentTimeMillis() - session.getStartTime());
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
        preventShowFloatingViewOnStop = true;
        Intent intent = new Intent(this, ConferenceParticipantListActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ADD_PARTICIPANT);
    }

    public void inviteNewParticipant() {
        preventShowFloatingViewOnStop = true;
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        ConferenceInfo conferenceInfo = ConferenceManager.getManager().getCurrentConferenceInfo();
        ConferenceInviteMessageContent invite = new ConferenceInviteMessageContent(session.getCallId(), ConferenceManager.getManager().getCurrentConferenceInfo().getOwner(), session.getTitle(), session.getDesc(), session.getStartTime(), session.isAudioOnly(), session.isDefaultAudience(), session.isAdvanced(), session.getPin(), conferenceInfo.getPassword());
        Intent intent = new Intent(this, ConferenceInviteActivity.class);
        intent.putExtra("inviteMessage", invite);
        startActivityForResult(intent, REQUEST_CODE_ADD_PARTICIPANT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT) {
            preventShowFloatingViewOnStop = false;
        }
    }
}
