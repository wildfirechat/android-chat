/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.asr;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;
import java.util.regex.Pattern;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.audio.PcmAudioRecorder;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;

/**
 * 实时语音输入管理器
 * <p>
 * 录音并实时推送到 wf-voice 识别（经过 asr-api 转发，内网测试时也可以直连）。wf-voice 每识别完一句返回这句的最终结果；
 * 开启 {@link Config#ENABLE_ASR_PARTIAL_RESULT} 后，说话过程中还会返回正在说的这句的中间结果。
 * 服务地址见 {@link Config#ASR_STREAM_SERVER_URL}。
 */
public class AsrManager {
    private static final String TAG = "AsrManager";
    private static final int MAX_RECORDING_DURATION_MS = 60 * 1000;  // 最长录音时长：60秒
    private static final int WAIT_EOS_TIMEOUT_MS = 10 * 1000;         // 停止录音后等待剩余识别结果的最长时间：10秒
    // 停止录音后服务端一直没有推送消息，认为已经识别完。服务端不支持 eos 指令时不会回复 [EOS]，靠它结束识别
    private static final int WAIT_EOS_IDLE_TIMEOUT_MS = 15 * 1000;
    // 停止录音后收到过识别结果，之后这么久没有新消息，认为已经识别完
    private static final int WAIT_EOS_IDLE_AFTER_RESULT_MS = 60 * 1000;

    // "Over" 热词（不区分大小写，支持中文），识别结果以它结尾时回调 onHotwordDetected
    private static final Pattern HOTWORD_OVER_PATTERN = Pattern.compile("(?i)(over|欧弗|结束)[，,.。\\s]*$");

    private enum State {
        IDLE,       // 空闲
        CONNECTING, // 连接中，已经开始录音或接收调用方提供的音频，音频先缓存
        RECORDING,  // 已连接，录音中
        FINISHING   // 已停止录音，等待剩余识别结果
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PcmAudioRecorder audioRecorder;
    private AsrWebSocketClient wsClient;
    private RecognitionCallback callback;
    private State state = State.IDLE;

    // 是否启用 "Over" 热词
    private boolean enableHotwordOver = false;

    // 本次识别已确定的文本，即各句的最终结果
    private String recognizedText = "";
    // 正在说的这句的中间结果，收到这句的最终结果后清空
    private String partialText = "";

    // 音频由调用方通过 feedAudioData 提供，而不是自己录音
    private boolean feedAudio;
    // 连接成功前已经停止录音或停止提供音频，连接成功后发送完缓存的音频再结束识别
    private boolean pendingStop;
    // 调用方提供的音频字节数，用于估算等待剩余识别结果的时间
    private long feedAudioBytes;

    private final Runnable maxDurationRunnable = () -> {
        Log.d(TAG, "达到最大录音时长，自动停止");
        stopRecognition();
    };

    private final Runnable waitEosTimeoutRunnable = () -> {
        if (state == State.CONNECTING) {
            Log.w(TAG, "连接语音识别服务超时");
            failRecognition("连接语音识别服务超时");
        } else {
            Log.w(TAG, "等待剩余识别结果超时，结束识别");
            finishRecognition();
        }
    };

    private final Runnable waitEosIdleRunnable = () -> {
        Log.w(TAG, "没有收到 [EOS]，按已返回的识别结果结束识别");
        finishRecognition();
    };

    /**
     * 识别回调接口（在主线程回调）
     */
    public interface RecognitionCallback {
        /**
         * 识别文本有更新：识别出新的一句，或者正在说的这句有了新的中间结果
         * @param text 本次识别到目前为止的全部文本
         */
        void onPartialResult(@NonNull String text);

        /**
         * 识别完成，之后不再回调
         * @param text 本次识别的全部文本，可能为空
         */
        void onFinalResult(@NonNull String text);

        /**
         * 出错，之后不再回调
         * @param message 错误信息
         */
        void onError(@NonNull String message);

        /**
         * 检测到热词，之后不再回调
         * @param hotword 检测到的热词（如 "Over"）
         * @param text 当前识别的文本（不包含热词）
         */
        void onHotwordDetected(@NonNull String hotword, @NonNull String text);
    }

    public AsrManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 设置是否启用 "Over" 热词
     * @param enable true 启用，false 禁用
     */
    public void setHotwordOverEnabled(boolean enable) {
        this.enableHotwordOver = enable;
    }

    /**
     * 开始语音识别，调用前需要已获得录音权限。会立即开始录音，连接识别服务期间录到的音频先缓存，连接成功后再发送
     * @param callback 回调接口
     */
    public void startRecognition(@NonNull RecognitionCallback callback) {
        start(callback, false);
    }

    /**
     * 开始语音识别，由调用方通过 {@link #feedAudioData(byte[])} 提供音频，提供完后调用 {@link #stopRecognition()}。
     * 连接识别服务期间提供的音频会先缓存，连接成功后再发送
     * @param callback 回调接口
     */
    public void startRecognitionWithAudioFeed(@NonNull RecognitionCallback callback) {
        start(callback, true);
    }

    private void start(@NonNull RecognitionCallback callback, boolean feedAudio) {
        if (state != State.IDLE) {
            Log.w(TAG, "正在识别中，无需重复开始");
            return;
        }
        String url = Config.getAsrStreamServerUrl();
        if (TextUtils.isEmpty(url)) {
            callback.onError("未配置语音识别服务地址");
            return;
        }

        this.callback = callback;
        this.feedAudio = feedAudio;
        state = State.CONNECTING;
        recognizedText = "";
        partialText = "";

        wsClient = new AsrWebSocketClient(new AsrWebSocketClient.Callback() {
            @Override
            public void onConnected() {
                state = State.RECORDING;
                if (pendingStop) {
                    pendingStop = false;
                    stopRecognition();
                }
            }

            @Override
            public void onPartialResult(@NonNull String text) {
                delayIdleFinish();
                partialText = text;
                AsrManager.this.callback.onPartialResult(getText());
            }

            @Override
            public void onResult(@NonNull String text) {
                delayIdleFinish();
                handleSentenceResult(text);
            }

            @Override
            public void onEos() {
                finishRecognition();
            }

            @Override
            public void onError(@NonNull String error) {
                Log.e(TAG, "语音识别服务错误: " + error);
                if (state == State.FINISHING) {
                    // 已经停止录音，保留已识别出的文本
                    finishRecognition();
                } else {
                    failRecognition(error);
                }
            }
        });
        if (!feedAudio) {
            // 获取认证码、连接识别服务可能要一两秒，用户点完就开始说话。先开始录音，音频由 wsClient 缓存到连接成功后发送
            startAudioRecording();
        }
        // wf-voice 要求每个连接的 clientId 唯一，并会用作服务端录音文件名。连接 asr-api 时由 asr-api 重新生成
        String clientId = ChatManager.Instance().getUserId() + "-" + UUID.randomUUID().toString().replace("-", "");
        if (!AsrAuth.isAsrApiUrl(url)) {
            // 直连 wf-voice，不需要鉴权
            wsClient.connect(url, clientId, Config.ENABLE_ASR_PARTIAL_RESULT, null);
            return;
        }
        AsrWebSocketClient client = wsClient;
        AsrAuth.getAuthCode(new GeneralCallback2() {
            @Override
            public void onSuccess(String authCode) {
                // 获取认证码期间，识别可能已经停止或取消
                if (wsClient == client) {
                    client.connect(url, clientId, Config.ENABLE_ASR_PARTIAL_RESULT, authCode);
                }
            }

            @Override
            public void onFail(int errorCode) {
                if (wsClient == client) {
                    failRecognition("获取认证码失败: " + errorCode);
                }
            }
        });
    }

    /**
     * 停止录音或停止提供音频，剩余识别结果返回后回调 onFinalResult
     */
    public void stopRecognition() {
        if (state == State.CONNECTING) {
            if (pendingStop) {
                return;
            }
            boolean hasAudio = audioRecorder != null || feedAudioBytes > 0;
            stopAudioRecording();
            if (hasAudio) {
                // 连接成功后发送完缓存的音频再结束，一直连不上时超时
                Log.d(TAG, "停止录音，连接成功后再结束识别");
                pendingStop = true;
                mainHandler.removeCallbacks(maxDurationRunnable);
                mainHandler.removeCallbacks(waitEosTimeoutRunnable);
                mainHandler.postDelayed(waitEosTimeoutRunnable, WAIT_EOS_TIMEOUT_MS);
            } else {
                // 还没有音频
                finishRecognition();
            }
        } else if (state == State.RECORDING) {
            Log.d(TAG, "停止录音，等待剩余识别结果");
            state = State.FINISHING;
            mainHandler.removeCallbacks(maxDurationRunnable);
            stopAudioRecording();
            wsClient.sendEos();
            // 一次提供了较长的音频时，服务端需要更多时间识别。16kHz、16-bit 的音频每毫秒 32 字节，按音频时长增加等待时间
            long audioMs = feedAudioBytes / 32;
            mainHandler.removeCallbacks(waitEosTimeoutRunnable);
            mainHandler.postDelayed(waitEosTimeoutRunnable, WAIT_EOS_TIMEOUT_MS + audioMs / 2);
            mainHandler.postDelayed(waitEosIdleRunnable, WAIT_EOS_IDLE_TIMEOUT_MS + audioMs / 10);
        }
    }

    /**
     * 停止录音后又收到识别结果，重新计算没有新消息就结束识别的时间
     */
    private void delayIdleFinish() {
        if (state == State.FINISHING) {
            mainHandler.removeCallbacks(waitEosIdleRunnable);
            mainHandler.postDelayed(waitEosIdleRunnable, WAIT_EOS_IDLE_AFTER_RESULT_MS);
        }
    }

    /**
     * 提供音频数据，只在 {@link #startRecognitionWithAudioFeed(RecognitionCallback)} 之后有效，需要在主线程调用
     * @param pcmData 16kHz、16-bit、单声道 PCM
     */
    public void feedAudioData(@NonNull byte[] pcmData) {
        if (!feedAudio || pendingStop || (state != State.CONNECTING && state != State.RECORDING)) {
            return;
        }
        wsClient.sendAudioData(pcmData);
        feedAudioBytes += pcmData.length;
    }

    /**
     * 取消语音识别，丢弃还没返回的识别结果，之后不再回调
     */
    public void cancelRecognition() {
        if (state != State.IDLE) {
            Log.d(TAG, "取消识别");
            cleanup();
        }
    }

    /**
     * 是否正在识别，包括停止录音后等待剩余识别结果的阶段
     */
    public boolean isRecognizing() {
        return state != State.IDLE;
    }

    private void startAudioRecording() {
        AsrWebSocketClient client = wsClient;
        PcmAudioRecorder recorder = new PcmAudioRecorder(context);
        audioRecorder = recorder;
        boolean success = recorder.startRecording(new PcmAudioRecorder.OnAudioDataCallback() {
            @Override
            public void onAudioData(byte[] pcmData) {
                // 在录音线程回调，实时发送到服务端，还没连接成功时 client 会先缓存
                client.sendAudioData(pcmData);
            }

            @Override
            public void onError(String message) {
                // 可能在录音线程回调。忽略已经停止的录音报的错误，包括停止录音时录音线程退出前报的错误
                mainHandler.post(() -> {
                    if (audioRecorder == recorder) {
                        failRecognition("录音失败: " + message);
                    }
                });
            }
        });

        // 启动失败时 PcmAudioRecorder 会回调 onError，由 onError 结束识别
        if (success) {
            Log.d(TAG, "录音已开始");
            mainHandler.postDelayed(maxDurationRunnable, MAX_RECORDING_DURATION_MS);
        }
    }

    private void stopAudioRecording() {
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
            audioRecorder = null;
        }
    }

    private void handleSentenceResult(@NonNull String sentence) {
        partialText = "";
        recognizedText = join(recognizedText, sentence);

        if (enableHotwordOver && HOTWORD_OVER_PATTERN.matcher(recognizedText).find()) {
            Log.d(TAG, "检测到 Over 热词");
            String text = HOTWORD_OVER_PATTERN.matcher(recognizedText).replaceAll("").trim();
            RecognitionCallback cb = callback;
            cleanup();
            cb.onHotwordDetected("Over", text);
            return;
        }

        callback.onPartialResult(getText());
    }

    /**
     * 已确定的文本，加上正在说的这句的中间结果
     */
    @NonNull
    private String getText() {
        return join(recognizedText, partialText);
    }

    /**
     * 拼接两段识别文本，两段英文之间补一个空格
     */
    @NonNull
    private static String join(@NonNull String text, @NonNull String sentence) {
        if (!text.isEmpty() && !sentence.isEmpty()) {
            char last = text.charAt(text.length() - 1);
            char first = sentence.charAt(0);
            if (last < 128 && !Character.isWhitespace(last) && first < 128 && Character.isLetterOrDigit(first)) {
                return text + " " + sentence;
            }
        }
        return text + sentence;
    }

    private void finishRecognition() {
        if (state == State.IDLE) {
            return;
        }
        RecognitionCallback cb = callback;
        // wf-voice 会把句号替换成逗号，去掉结尾多余的逗号
        String text = getText().replaceAll("[，,]+$", "");
        cleanup();
        cb.onFinalResult(text);
    }

    private void failRecognition(@NonNull String message) {
        if (state == State.IDLE) {
            return;
        }
        RecognitionCallback cb = callback;
        cleanup();
        cb.onError(message);
    }

    private void cleanup() {
        state = State.IDLE;
        callback = null;
        mainHandler.removeCallbacks(maxDurationRunnable);
        mainHandler.removeCallbacks(waitEosTimeoutRunnable);
        mainHandler.removeCallbacks(waitEosIdleRunnable);
        stopAudioRecording();
        if (wsClient != null) {
            wsClient.disconnect();
            wsClient = null;
        }
        recognizedText = "";
        partialText = "";
        feedAudio = false;
        pendingStop = false;
        feedAudioBytes = 0;
    }
}
