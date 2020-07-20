/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package cn.wildfire.chat.kit.voip;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.webrtc.StatsReport;

import cn.wildfirechat.avenginekit.AVEngineKit;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class SingleCallActivity extends VoipBaseActivity {
    private static final String TAG = "P2PVideoActivity";
    private static final int REQUEST_CODE_DRAW_OVERLAY = 100;

    public static final String EXTRA_FROM_FLOATING_VIEW = "fromFloatingView";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        init();
    }

    private AVEngineKit.CallSessionCallback currentCallback;

    private void init() {
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session == null || AVEngineKit.CallState.Idle == session.getState()) {
            finishFadeout();
            return;
        }

        Fragment fragment;
        if (session.isAudioOnly()) {
            fragment = new SingleAudioFragment();
        } else {
            fragment = new SingleVideoFragment();
        }

        currentCallback = (AVEngineKit.CallSessionCallback) fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commit();
    }

    // Activity interfaces
    @Override
    public void onStop() {
        super.onStop();
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
//            session.stopVideoSource();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
//      session.startVideoSource();
        }
    }

    @Override
    protected void onDestroy() {
        // TODO do not endCall
//        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
//        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
//            session.endCall();
//        }
        super.onDestroy();
    }

    public AVEngineKit getEngineKit() {
        return gEngineKit;
    }

    @Override
    public void didError(String error) {
        postAction(() -> currentCallback.didError(error));
    }

    @Override
    public void didGetStats(StatsReport[] reports) {
        postAction(() -> currentCallback.didGetStats(reports));
    }

    @Override
    public void didChangeMode(boolean audioOnly) {
        postAction(() -> {
            currentCallback.didChangeMode(audioOnly);
            if (audioOnly) {
                SingleAudioFragment fragment = new SingleAudioFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
                currentCallback = fragment;
            } else {
                // never called
            }

        });
    }

    @Override
    public void didChangeState(AVEngineKit.CallState state) {
        postAction(() -> currentCallback.didChangeState(state));
    }

    @Override
    public void didCreateLocalVideoTrack() {
        postAction(() -> currentCallback.didCreateLocalVideoTrack());
    }

    @Override
    public void didReceiveRemoteVideoTrack(String s) {
        postAction(() -> currentCallback.didReceiveRemoteVideoTrack(s));
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        postAction(() -> currentCallback.didReportAudioVolume(userId, volume));
    }

    public void audioAccept() {
        if (currentCallback instanceof SingleAudioFragment) {
            return;
        }
        SingleAudioFragment fragment = new SingleAudioFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit();
        currentCallback = fragment;

        AVEngineKit.CallSession session = gEngineKit.getCurrentSession();
        if (session != null) {
            if (session.getState() == AVEngineKit.CallState.Incoming) {
                session.answerCall(true);
            } else if (session.getState() == AVEngineKit.CallState.Connected) {
                session.setAudioOnly(true);
            }
        } else {
            finishFadeout();
        }
    }

    public void audioCall() {
        audioAccept();
    }

}
