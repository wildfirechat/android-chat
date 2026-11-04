/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

/**
 * Created by heavyrainlee on 24/02/2018.
 */

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;

import cn.wildfire.chat.kit.WfcUIKit;

public class AsyncPlayer {
    private static final int PLAY = 1;
    private static final int STOP = 2;
    private static final boolean mDebug = false;

    private static final class Command {
        int code;
        Context context;
        Uri uri;
        boolean looping;
        int stream;
        long requestTime;

        public String toString() {
            return "{ code=" + code + " looping=" + looping + " stream=" + stream + " uri=" + uri + " }";
        }
    }

    private final LinkedList<Command> mCmdQueue = new LinkedList<Command>();

    private void startSound(Command cmd) {

        try {
            MediaPlayer player = new MediaPlayer();
            player.setAudioStreamType(cmd.stream);
            player.setDataSource(cmd.context, cmd.uri);
            player.setLooping(cmd.looping);
            player.setAudioAttributes(isRingtone(cmd) ? RINGTONE_ATTRIBUTES : SIGNALLING_ATTRIBUTES);
            player.setOnErrorListener((mediaPlayer, i, i1) -> {
                Log.e(mTag, "play ring error " + i + " " + i1);
                return false;
            });
            player.prepare();
            player.start();
            if (mPlayer != null) {
                mPlayer.stop();
                mPlayer.release();
            }
            mPlayer = player;
        } catch (IOException e) {
            Log.w(mTag, "error loading sound for " + cmd.uri, e);
        } catch (IllegalStateException e) {
            Log.w(mTag, "IllegalStateException (content provider died?) " + cmd.uri, e);
        }
    }

    private final class Thread extends java.lang.Thread {
        Thread() {
            super("AsyncPlayer-" + mTag);
        }

        public void run() {
            while (true) {
                Command cmd = null;

                synchronized (mCmdQueue) {

                    cmd = (Command) mCmdQueue.removeFirst();
                }

                AudioManager audioManager = ((AudioManager) WfcUIKit.getWfcUIKit().getApplication().getSystemService(Context.AUDIO_SERVICE));
                switch (cmd.code) {
                    case PLAY:
                        savedAudioMode = audioManager.getMode();
                        audioModeChanged = true;
                        // 来电时音视频引擎已把音频模式切到 MODE_IN_COMMUNICATION，单人语音通话还会选中听筒，
                        // 这个模式下铃声也会跟着走听筒，声音很小。所以来电响铃期间切回 MODE_NORMAL，让铃声外放；
                        // 呼出回铃音保持 MODE_IN_COMMUNICATION，跟随通话音量和听筒/扬声器切换
                        audioManager.setMode(isRingtone(cmd) ? AudioManager.MODE_NORMAL : AudioManager.MODE_IN_COMMUNICATION);
                        startSound(cmd);
                        if (isRingtone(cmd)) {
                            startVibrateIfNeeded(cmd.context, audioManager);
                        }
                        break;
                    case STOP:

                        Log.e(mTag, "STOP CMD");
                        stopVibrate();
                        if (mPlayer != null) {
                            Log.e("AsyncPlayer", "mediaPlayer stop & release");
                            mPlayer.stop();
                            mPlayer.release();
                            mPlayer = null;
                        } else {
                            Log.w(mTag, "STOP command without a player");
                        }
                        // 播放失败时也要恢复，否则来电接听后会停留在 MODE_NORMAL
                        if (audioModeChanged) {
                            audioManager.setMode(savedAudioMode);
                            audioModeChanged = false;
                        }
                        break;
                }

                synchronized (mCmdQueue) {
                    if (mCmdQueue.size() == 0) {

                        mThread = null;
                        releaseWakeLock();
                        return;
                    }
                }
            }
        }
    }

    private String mTag;
    private Thread mThread;
    private MediaPlayer mPlayer;
    private PowerManager.WakeLock mWakeLock;

    private int savedAudioMode = AudioManager.MODE_NORMAL;
    private boolean audioModeChanged;
    private Vibrator mVibrator;

    // 来电铃声：走铃声通道，外放，跟随系统铃声音量，遵守静音/振动模式
    private static final AudioAttributes RINGTONE_ATTRIBUTES = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build();

    // 呼出回铃音：走通话信令通道，跟随通话音量和听筒/扬声器切换
    private static final AudioAttributes SIGNALLING_ATTRIBUTES = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build();

    private static boolean isRingtone(Command cmd) {
        return cmd.stream == AudioManager.STREAM_RING;
    }

    // 铃声通道在振动模式下没有声音，来电提醒改为振动，避免完全无提醒
    private void startVibrateIfNeeded(Context context, AudioManager audioManager) {
        if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
            return;
        }
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        vibrator.vibrate(new long[]{0, 1000, 1000}, 0, RINGTONE_ATTRIBUTES);
        mVibrator = vibrator;
    }

    private void stopVibrate() {
        if (mVibrator != null) {
            mVibrator.cancel();
            mVibrator = null;
        }
    }

    private int mState = STOP;

    public AsyncPlayer(String tag) {
        if (tag != null) {
            mTag = tag;
        } else {
            mTag = "AsyncPlayer";
        }
    }

    public void play(Context context, Uri uri, boolean looping, int stream) {
        Command cmd = new Command();
        cmd.requestTime = SystemClock.uptimeMillis();
        cmd.code = PLAY;
        cmd.context = context;
        cmd.uri = uri;
        cmd.looping = looping;
        cmd.stream = stream;
        synchronized (mCmdQueue) {
            enqueueLocked(cmd);
            mState = PLAY;
        }
    }

    public void stop() {
        Log.e(mTag, "stop");
        synchronized (mCmdQueue) {
            if (mState != STOP) {
                Command cmd = new Command();
                cmd.requestTime = SystemClock.uptimeMillis();
                cmd.code = STOP;
                enqueueLocked(cmd);
                mState = STOP;
            }
        }
    }

    private void enqueueLocked(Command cmd) {
        mCmdQueue.add(cmd);
        if (mThread == null) {
            acquireWakeLock();
            mThread = new Thread();
            mThread.start();
        }
    }

    public void setUsesWakeLock(Context context) {
        if (mWakeLock != null || mThread != null) {
            throw new RuntimeException("assertion failed mWakeLock=" + mWakeLock + " mThread=" + mThread);
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mTag);
    }

    private void acquireWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }
}
