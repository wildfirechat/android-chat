/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfirechat.avenginekit.AVEngineKit;

/**
 * 监听系统电话（普通电话）状态，在普通电话接通（来电接听）或拨出（去电）时打断正在进行的音视频通话。
 * <p>
 * 由 {@link VoipCallService} 在通话期间持有，服务销毁时释放。
 * <p>
 * 两条通道，二选一：
 * <ul>
 * <li>主通道：{@code TelephonyCallback.CallStateListener}（API 31+）/ {@link PhoneStateListener}（更低版本）。
 * 需要 {@link Manifest.permission#READ_PHONE_STATE} 权限。{@code CALL_STATE_OFFHOOK} 同时覆盖
 * 来电接听（RINGING -&gt; OFFHOOK）和去电拨出（IDLE -&gt; OFFHOOK），一个回调即可实现来电打断和去电打断。</li>
 * <li>兜底通道：轮询 {@link AudioManager#getMode()}，不需要任何权限，在主通道不可用（权限未授予）时启用。
 * 系统电话会把音频模式置为 {@link AudioManager#MODE_IN_CALL}，而 VoIP 用的是
 * {@link AudioManager#MODE_IN_COMMUNICATION}，不会误判。可靠性略低于主通道。</li>
 * </ul>
 * <p>
 * 注意：不要用 {@code PROCESS_OUTGOING_CALLS} 权限和 {@code ACTION_NEW_OUTGOING_CALL} 广播来做去电打断，
 * 二者在 API 29 已废弃，Android 10 及以上不再投递给三方应用。
 */
public class PstnCallMonitor {
    private static final String TAG = "PstnCallMonitor";
    private static final long AUDIO_MODE_POLL_INTERVAL = 1000;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TelephonyManager telephonyManager;
    // API 31+ 为 TelephonyCallback，更低版本为 PhoneStateListener。用 Object 持有，避免低版本上加载不存在的类
    private Object telephonyListener;
    private Runnable audioModePoller;

    private boolean pstnCallOngoing;
    // 会议被打断时，本地麦克风是否是由本监听器静音的。只有是，普通电话结束后才需要恢复
    private boolean mutedByMonitor;

    public PstnCallMonitor(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 必须在主线程调用。
     */
    public void start() {
        if (!Config.ENABLE_PSTN_CALL_INTERRUPT) {
            return;
        }
        if (telephonyListener != null || audioModePoller != null) {
            return;
        }
        if (!registerTelephonyListener()) {
            startAudioModePolling();
        }
    }

    public void stop() {
        unregisterTelephonyListener();
        stopAudioModePolling();
        pstnCallOngoing = false;
        mutedByMonitor = false;
    }

    private boolean registerTelephonyListener() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_PHONE_STATE not granted, fallback to audio mode polling");
            return false;
        }
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                CallStateCallback callback = new CallStateCallback();
                telephonyManager.registerTelephonyCallback(ContextCompat.getMainExecutor(context), callback);
                telephonyListener = callback;
            } else {
                PhoneStateListener listener = new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String phoneNumber) {
                        PstnCallMonitor.this.onCallStateChanged(state);
                    }
                };
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
                telephonyListener = listener;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "register telephony listener failed", e);
            telephonyListener = null;
            return false;
        }
    }

    private void unregisterTelephonyListener() {
        if (telephonyListener == null || telephonyManager == null) {
            telephonyListener = null;
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.unregisterTelephonyCallback((TelephonyCallback) telephonyListener);
            } else {
                telephonyManager.listen((PhoneStateListener) telephonyListener, PhoneStateListener.LISTEN_NONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "unregister telephony listener failed", e);
        }
        telephonyListener = null;
    }

    private void startAudioModePolling() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        audioModePoller = new Runnable() {
            @Override
            public void run() {
                onCallStateChanged(audioManager.getMode() == AudioManager.MODE_IN_CALL
                    ? TelephonyManager.CALL_STATE_OFFHOOK
                    : TelephonyManager.CALL_STATE_IDLE);
                handler.postDelayed(this, AUDIO_MODE_POLL_INTERVAL);
            }
        };
        handler.postDelayed(audioModePoller, AUDIO_MODE_POLL_INTERVAL);
    }

    private void stopAudioModePolling() {
        if (audioModePoller != null) {
            handler.removeCallbacks(audioModePoller);
            audioModePoller = null;
        }
    }

    private void onCallStateChanged(int state) {
        switch (state) {
            case TelephonyManager.CALL_STATE_OFFHOOK:
                // 来电接听和去电拨出都会到达 OFFHOOK
                onPstnCallStarted();
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                onPstnCallEnded();
                break;
            case TelephonyManager.CALL_STATE_RINGING:
            default:
                // 仅响铃时不打断，否则用户拒接普通来电之后，音视频通话已经被挂断了
                break;
        }
    }

    private void onPstnCallStarted() {
        if (pstnCallOngoing) {
            return;
        }

        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            // 还没有通话，不要置位，否则等通话真正建立时就不会再打断了
            return;
        }
        pstnCallOngoing = true;

        if (session.isConference() && Config.PSTN_INTERRUPT_MUTE_CONFERENCE_ONLY) {
            // 会议是多人场景，直接挂断代价太大，只静音本地麦克风
            if (!session.isAudioMuted()) {
                Log.d(TAG, "pstn call started, mute conference audio");
                session.muteAudio(true);
                mutedByMonitor = true;
                toast(R.string.pstn_interrupt_conference_muted);
            }
        } else {
            Log.d(TAG, "pstn call started, end current call session");
            session.endCall(AVEngineKit.CallEndReason.Interrupted);
        }
    }

    private void onPstnCallEnded() {
        if (!pstnCallOngoing) {
            return;
        }
        pstnCallOngoing = false;
        if (!mutedByMonitor) {
            return;
        }
        mutedByMonitor = false;

        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }
        Log.d(TAG, "pstn call ended, unmute conference audio");
        session.muteAudio(false);
        toast(R.string.pstn_interrupt_conference_unmuted);
    }

    private void toast(int resId) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private class CallStateCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            PstnCallMonitor.this.onCallStateChanged(state);
        }
    }
}
