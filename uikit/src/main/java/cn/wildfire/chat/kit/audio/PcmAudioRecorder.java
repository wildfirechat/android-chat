/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.Manifest;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * PCM 音频录制器，用于 FunASR 语音识别
 * 采集 16kHz, 16-bit, 单声道 PCM 音频数据
 */
public class PcmAudioRecorder implements AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "PcmAudioRecorder";

    // 音频参数配置（与 FunASR 要求一致）
    private static final int SAMPLE_RATE = 16000;  // 16kHz 采样率
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;  // 单声道
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;  // 16-bit
    private static final int CHUNK_SIZE = 960;  // 每次读取的字节数（对应 60ms 音频）

    private Context context;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private AudioRecord audioRecord;
    private int bufferSize;
    private boolean isRecording = false;
    private RecordingThread recordingThread;
    private OnAudioDataCallback callback;

    /**
     * 音频数据回调接口
     */
    public interface OnAudioDataCallback {
        /**
         * 音频数据回调（在录音线程中调用）
         * @param pcmData PCM 音频数据，长度为 CHUNK_SIZE (960 字节)
         */
        void onAudioData(byte[] pcmData);

        /**
         * 错误回调
         * @param message 错误信息
         */
        void onError(String message);
    }

    public PcmAudioRecorder(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 开始录音
     * @param callback 音频数据回调
     * @return 是否成功开始录音
     */
    public boolean startRecording(@NonNull OnAudioDataCallback callback) {
        this.callback = callback;

        // 请求音频焦点
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();

            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this, new Handler())
                .build();

            int result = audioManager.requestAudioFocus(audioFocusRequest);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                callback.onError("无法获取音频焦点");
                return false;
            }
        } else {
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                callback.onError("无法获取音频焦点");
                return false;
            }
        }

        // 计算 AudioRecord 的最小缓冲区大小
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            callback.onError("不支持此音频配置");
            releaseAudioFocus();
            return false;
        }

        // 确保 bufferSize 至少是 CHUNK_SIZE 的两倍，避免缓冲区溢出
        if (bufferSize < CHUNK_SIZE * 2) {
            bufferSize = CHUNK_SIZE * 2;
        }

        try {
            // 创建 AudioRecord（使用旧构造函数以兼容 Android 7.0+）
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                callback.onError("AudioRecord 初始化失败");
                releaseAudioFocus();
                return false;
            }

            // 开始录音
            audioRecord.startRecording();
            isRecording = true;

            // 启动录音线程
            recordingThread = new RecordingThread();
            recordingThread.start();

            Log.d(TAG, "录音已开始: " + SAMPLE_RATE + "Hz, 16-bit, 单声道");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("启动录音失败: " + e.getMessage());
            releaseAudioFocus();
            return false;
        }
    }

    /**
     * 停止录音
     */
    public void stopRecording() {
        isRecording = false;

        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (IllegalStateException e) {
                Log.e(TAG, "停止录音失败", e);
            }
            audioRecord.release();
            audioRecord = null;
        }

        // 等待录音线程结束
        if (recordingThread != null) {
            try {
                recordingThread.join(500);  // 最多等待 500ms
            } catch (InterruptedException e) {
                Log.e(TAG, "等待录音线程结束被中断", e);
            }
            recordingThread = null;
        }

        releaseAudioFocus();
        Log.d(TAG, "录音已停止");
    }

    /**
     * 释放音频焦点
     */
    private void releaseAudioFocus() {
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
                audioFocusRequest = null;
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocus(this);
            }
            audioManager = null;
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "音频焦点变化: " + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            if (callback != null) {
                callback.onError("失去音频焦点");
            }
            stopRecording();
        }
    }

    /**
     * 录音线程，持续读取音频数据
     */
    private class RecordingThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            byte[] buffer = new byte[CHUNK_SIZE];

            Log.d(TAG, "录音线程已启动");

            while (isRecording && audioRecord != null) {
                int readSize = audioRecord.read(buffer, 0, CHUNK_SIZE);

                if (readSize > 0) {
                    // 成功读取音频数据
                    if (readSize == CHUNK_SIZE) {
                        // 完整的一帧数据
                        if (callback != null) {
                            callback.onAudioData(buffer);
                        }
                    } else {
                        // 读取不足 CHUNK_SIZE，填充 0
                        byte[] paddedBuffer = new byte[CHUNK_SIZE];
                        System.arraycopy(buffer, 0, paddedBuffer, 0, readSize);
                        if (callback != null) {
                            callback.onAudioData(paddedBuffer);
                        }
                    }
                } else if (readSize == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "AudioRecord 读取错误: ERROR_INVALID_OPERATION");
                    if (callback != null) {
                        callback.onError("AudioRecord 读取错误");
                    }
                    break;
                } else if (readSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "AudioRecord 读取错误: ERROR_BAD_VALUE");
                    if (callback != null) {
                        callback.onError("AudioRecord 参数错误");
                    }
                    break;
                } else {
                    // readSize == 0 或其他错误，继续尝试
                    Log.w(TAG, "AudioRecord 读取返回: " + readSize);
                }
            }

            Log.d(TAG, "录音线程已结束");
        }
    }
}
