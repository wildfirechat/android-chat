package cn.wildfire.chat.kit.voip;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.webrtc.StatsReport;

import cn.wildfirechat.avenginekit.AVEngineKit;

public class MultiCallActivity extends VoipBaseActivity {

    private AVEngineKit.CallSessionCallback currentCallSessionCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null) {
            finish();
            return;
        }
        session.setCallback(this);

        Fragment fragment;
        if (session.isAudioOnly()) {
            fragment = new MultiCallAudioFragment();
        } else {
            fragment = new MultiCallVideoFragment();
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
    public void didParticipantJoined(String userId) {
        postAction(() -> {
            currentCallSessionCallback.didParticipantJoined(userId);
        });
    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason) {
        postAction(() -> {
            currentCallSessionCallback.didParticipantLeft(userId, callEndReason);
        });
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

    @Override
    public void didCreateLocalVideoTrack() {
        postAction(() -> {
            currentCallSessionCallback.didCreateLocalVideoTrack();
        });
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId) {
        postAction(() -> {
            currentCallSessionCallback.didReceiveRemoteVideoTrack(userId);
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
}
