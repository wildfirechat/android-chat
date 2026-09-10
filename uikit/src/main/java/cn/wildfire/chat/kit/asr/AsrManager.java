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
    private static final int WAIT_EOS_TIMEOUT_MS = 8 * 1000;         // 停止录音后等待剩余识别结果的最长时间：8秒

    // "Over" 热词（不区分大小写，支持中文），识别结果以它结尾时回调 onHotwordDetected
    private static final Pattern HOTWORD_OVER_PATTERN = Pattern.compile("(?i)(over|欧弗|结束)[，,.。\\s]*$");

    private enum State {
        IDLE,       // 空闲
        CONNECTING, // 连接中
        RECORDING,  // 录音中
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

    private final Runnable maxDurationRunnable = () -> {
        Log.d(TAG, "达到最大录音时长，自动停止");
        stopRecognition();
    };

    private final Runnable waitEosTimeoutRunnable = () -> {
        Log.w(TAG, "等待剩余识别结果超时，结束识别");
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
     * 开始语音识别，调用前需要已获得录音权限
     * @param callback 回调接口
     */
    public void startRecognition(@NonNull RecognitionCallback callback) {
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
        state = State.CONNECTING;
        recognizedText = "";
        partialText = "";

        wsClient = new AsrWebSocketClient(new AsrWebSocketClient.Callback() {
            @Override
            public void onConnected() {
                startAudioRecording();
            }

            @Override
            public void onPartialResult(@NonNull String text) {
                partialText = text;
                AsrManager.this.callback.onPartialResult(getText());
            }

            @Override
            public void onResult(@NonNull String text) {
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
     * 停止录音，剩余识别结果返回后回调 onFinalResult
     */
    public void stopRecognition() {
        if (state == State.CONNECTING) {
            // 还没开始录音
            finishRecognition();
        } else if (state == State.RECORDING) {
            Log.d(TAG, "停止录音，等待剩余识别结果");
            state = State.FINISHING;
            mainHandler.removeCallbacks(maxDurationRunnable);
            audioRecorder.stopRecording();
            audioRecorder = null;
            wsClient.sendEos();
            mainHandler.postDelayed(waitEosTimeoutRunnable, WAIT_EOS_TIMEOUT_MS);
        }
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
        audioRecorder = new PcmAudioRecorder(context);
        boolean success = audioRecorder.startRecording(new PcmAudioRecorder.OnAudioDataCallback() {
            @Override
            public void onAudioData(byte[] pcmData) {
                // 在录音线程回调，实时发送到服务端
                client.sendAudioData(pcmData);
            }

            @Override
            public void onError(String message) {
                // 可能在录音线程回调。忽略已结束的识别，以及停止录音后录音线程退出前报的错误
                mainHandler.post(() -> {
                    if (wsClient == client && state != State.FINISHING) {
                        failRecognition("录音失败: " + message);
                    }
                });
            }
        });

        // 启动失败时 PcmAudioRecorder 会回调 onError，由 onError 结束识别
        if (success) {
            Log.d(TAG, "录音已开始");
            state = State.RECORDING;
            mainHandler.postDelayed(maxDurationRunnable, MAX_RECORDING_DURATION_MS);
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
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
            audioRecorder = null;
        }
        if (wsClient != null) {
            wsClient.disconnect();
            wsClient = null;
        }
        recognizedText = "";
        partialText = "";
    }
}
