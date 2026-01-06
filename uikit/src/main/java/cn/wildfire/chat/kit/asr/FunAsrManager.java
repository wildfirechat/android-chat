/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.asr;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wildfire.chat.kit.audio.PcmAudioRecorder;

/**
 * FunASR 语音识别管理器
 * 协调音频录制和 WebSocket 通信，处理识别结果
 */
public class FunAsrManager {
    private static final String TAG = "FunAsrManager";
    private static final int MAX_RECORDING_DURATION_MS = 60 * 1000;  // 最长录音时长：60秒
    private static final int WAIT_FINAL_RESULT_DELAY_MS = 3000;      // 等待最终结果的延迟：3秒

    private Context context;
    private PcmAudioRecorder audioRecorder;
    private FunAsrWebSocketClient wsClient;
    private Handler mainHandler;

    // 状态管理
    private boolean isRecognizing = false;
    private long startTime = 0;  // 录音开始时间

    // 热词配置
    private boolean enableHotwordOver = false;  // 是否启用 "Over" 热词
    private String hotwordsConfig = null;      // 热词配置 JSON 字符串

    // 识别结果
    private StringBuilder onlineText = new StringBuilder();   // online 模式识别结果
    private StringBuilder offlineText = new StringBuilder();  // offline 模式识别结果

    // 超时处理
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;

    /**
     * 识别回调接口
     */
    public interface RecognitionCallback {
        /**
         * 开始录音（在主线程回调）
         */
        void onStartRecording();

        /**
         * 中间结果（实时识别，在主线程回调）
         * @param text 当前识别的文本
         */
        void onPartialResult(@NonNull String text);

        /**
         * 最终结果（识别完成，在主线程回调）
         * @param text 最终识别的文本
         */
        void onFinalResult(@NonNull String text);

        /**
         * 停止录音（在主线程回调）
         */
        void onStopRecording();

        /**
         * 错误（在主线程回调）
         * @param message 错误信息
         */
        void onError(@NonNull String message);

        /**
         * 检测到热词（在主线程回调）
         * @param hotword 检测到的热词（如 "Over"）
         * @param text 当前识别的文本（不包含热词）
         */
        void onHotwordDetected(@NonNull String hotword, @NonNull String text);
    }

    public FunAsrManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.timeoutHandler = new Handler(Looper.getMainLooper());
        this.audioRecorder = new PcmAudioRecorder(context);
        this.wsClient = new FunAsrWebSocketClient();

        // 默认启用 "Over" 热词，权重为 50
        if (enableHotwordOver) {
            try {
                org.json.JSONObject hotwords = new org.json.JSONObject();
                hotwords.put("Over", 50);
                this.hotwordsConfig = hotwords.toString();
                Log.d(TAG, "启用 Over 热词: " + hotwordsConfig);
            } catch (Exception e) {
                Log.e(TAG, "构造热词配置失败", e);
            }
        }
    }

    /**
     * 设置是否启用 "Over" 热词
     * @param enable true 启用，false 禁用
     */
    public void setHotwordOverEnabled(boolean enable) {
        this.enableHotwordOver = enable;
        if (enable) {
            try {
                org.json.JSONObject hotwords = new org.json.JSONObject();
                hotwords.put("Over", 50);
                this.hotwordsConfig = hotwords.toString();
                Log.d(TAG, "启用 Over 热词");
            } catch (Exception e) {
                Log.e(TAG, "构造热词配置失败", e);
            }
        } else {
            this.hotwordsConfig = null;
            Log.d(TAG, "禁用 Over 热词");
        }
    }

    /**
     * 设置自定义热词配置
     * @param hotwordsJson 热词 JSON 字符串，格式为 {"word": weight}
     *                      例如：{"Over": 50, "确认": 30}
     */
    public void setHotwords(@Nullable String hotwordsJson) {
        this.hotwordsConfig = hotwordsJson;
    }

    /**
     * 开始语音识别
     * @param callback 回调接口
     */
    public void startRecognition(@NonNull RecognitionCallback callback) {
        if (isRecognizing) {
            Log.w(TAG, "正在识别中，无需重复开始");
            return;
        }

        isRecognizing = true;
        onlineText.setLength(0);
        offlineText.setLength(0);
        startTime = System.currentTimeMillis();

        // 设置超时检测
        setupTimeoutDetection(callback);

        // 第一步：连接 WebSocket
        wsClient.connect(new FunAsrWebSocketClient.AsrResponseCallback() {
            @Override
            public void onConnectionStateChanged(FunAsrWebSocketClient.State state) {
                if (state == FunAsrWebSocketClient.State.CONNECTED) {
                    Log.d(TAG, "WebSocket 已连接，发送初始化配置");
                    // 第二步：发送初始化配置（包含热词）
                    wsClient.sendInitConfig("2pass", hotwordsConfig);

                    // 第三步：开始录音
                    startAudioRecording(callback);
                } else if (state == FunAsrWebSocketClient.State.ERROR) {
                    Log.e(TAG, "WebSocket 连接失败");
                    cleanup();
                    callback.onError("连接服务器失败");
                }
            }

            @Override
            public void onRecognitionResult(String text, boolean isFinal, String mode) {
                // 收到识别结果，重置超时检测
                resetTimeoutDetection(callback);

                // 处理识别结果
                handleRecognitionResult(text, isFinal, mode, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "WebSocket 错误: " + error);
                callback.onError(error);
                cleanup();
            }
        });
    }

    /**
     * 停止语音识别
     * @param callback 回调接口
     */
    public void stopRecognition(@NonNull RecognitionCallback callback) {
        if (!isRecognizing) {
            Log.w(TAG, "当前没有在识别");
            return;
        }

        Log.d(TAG, "停止识别");

        // 停止录音
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
        }

        // 通知停止录音
        callback.onStopRecording();

        // 发送停止消息给服务器
        if (wsClient != null) {
            wsClient.sendStopMessage();
        }

        // 等待最终结果
        mainHandler.postDelayed(() -> {
            Log.d(TAG, "等待最终结果超时，结束识别");
            finishRecognition(callback);
        }, WAIT_FINAL_RESULT_DELAY_MS);
    }

    /**
     * 取消语音识别（不发送停止消息，直接断开）
     */
    public void cancelRecognition() {
        if (!isRecognizing) {
            return;
        }

        Log.d(TAG, "取消识别");

        // 停止录音
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
        }

        // 断开 WebSocket
        if (wsClient != null) {
            wsClient.disconnect(1000, "用户取消");
        }

        cleanup();
    }

    /**
     * 开始音频录制
     * @param callback 回调接口
     */
    private void startAudioRecording(@NonNull RecognitionCallback callback) {
        boolean success = audioRecorder.startRecording(new PcmAudioRecorder.OnAudioDataCallback() {
            @Override
            public void onAudioData(byte[] pcmData) {
                // 实时音频数据回调，发送到服务器
                if (wsClient != null) {
                    wsClient.sendAudioData(pcmData);
                }

                // 检查是否超时
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime >= MAX_RECORDING_DURATION_MS) {
                    Log.d(TAG, "达到最大录音时长，自动停止");
                    mainHandler.post(() -> stopRecognition(callback));
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "录音错误: " + message);
                callback.onError("录音失败: " + message);
                cleanup();
            }
        });

        if (success) {
            Log.d(TAG, "录音已开始");
            callback.onStartRecording();
        } else {
            Log.e(TAG, "启动录音失败");
            callback.onError("启动录音失败");
            cleanup();
        }
    }

    /**
     * 处理识别结果
     * @param text 识别文本
     * @param isFinal 是否最终结果
     * @param mode 识别模式
     * @param callback 回调接口
     */
    private void handleRecognitionResult(@NonNull String text, boolean isFinal, @NonNull String mode,
                                         @NonNull RecognitionCallback callback) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // 根据 mode 处理不同的识别结果
        if ("2pass-offline".equals(mode) || "offline".equals(mode)) {
            // 离线模式的最终结果，追加到 offlineText
            offlineText.append(text);
        } else {
            // online 模式或中间结果，追加到 onlineText
            onlineText.append(text);
        }

        // 决定显示哪个结果
        StringBuilder displayText;
        if (offlineText.length() > 0) {
            displayText = offlineText;
        } else {
            displayText = onlineText;
        }

        String resultText = displayText.toString();

        // 检测热词 "Over"（不区分大小写，支持中英文）
        if (enableHotwordOver && detectHotwordOver(resultText)) {
            Log.d(TAG, "检测到 Over 热词");
            // 移除 "Over" 及其前后空格
            String textWithoutHotword = removeHotwordOver(resultText);
            // 回调热词检测
            callback.onHotwordDetected("Over", textWithoutHotword);
            return;
        }

        // 回调结果
        if (isFinal) {
            Log.d(TAG, "最终识别结果: " + resultText);
            callback.onFinalResult(resultText);
        } else {
            Log.d(TAG, "中间识别结果: " + resultText);
            callback.onPartialResult(resultText);
        }
    }

    /**
     * 检测是否包含 "Over" 热词（不区分大小写）
     * @param text 识别文本
     * @return 是否包含 "Over"
     */
    private boolean detectHotwordOver(@NonNull String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // 转换为小写进行匹配
        String lowerText = text.toLowerCase().trim();
        // 检测 "over"、"欧弗"、"结束" 等关键词
        return lowerText.endsWith("over")
            || lowerText.endsWith("欧弗")
            || lowerText.endsWith("结束")
            || lowerText.matches(".*over[，,.。\\s]*$");
    }

    /**
     * 移除 "Over" 热词
     * @param text 原始文本
     * @return 移除 "Over" 后的文本
     */
    @NonNull
    private String removeHotwordOver(@NonNull String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 移除结尾的 "Over" 及其标点符号和空格
        String result = text.replaceAll("(?i)(over|欧弗|结束)[，,.。\\s]*$", "");
        return result.trim();
    }

    /**
     * 完成识别（清理资源）
     * @param callback 回调接口
     */
    private void finishRecognition(@NonNull RecognitionCallback callback) {
        // 断开 WebSocket
        if (wsClient != null) {
            wsClient.disconnect(1000, "识别完成");
        }

        cleanup();

        // 如果有最终结果，回调
        String finalText = offlineText.length() > 0 ? offlineText.toString() : onlineText.toString();
        if (!finalText.isEmpty()) {
            callback.onFinalResult(finalText);
        }
    }

    /**
     * 设置超时检测
     * @param callback 回调接口
     */
    private void setupTimeoutDetection(@NonNull RecognitionCallback callback) {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }

        timeoutRunnable = () -> {
            if (isRecognizing) {
                Log.w(TAG, "识别超时（30秒无结果）");
                callback.onError("识别超时，请重试");
                cancelRecognition();
            }
        };

        // 30秒无识别结果则超时
        timeoutHandler.postDelayed(timeoutRunnable, 30000);
    }

    /**
     * 重置超时检测
     * @param callback 回调接口
     */
    private void resetTimeoutDetection(@NonNull RecognitionCallback callback) {
        setupTimeoutDetection(callback);
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        isRecognizing = false;

        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }

        onlineText.setLength(0);
        offlineText.setLength(0);
    }

    /**
     * 获取是否正在识别
     * @return 是否正在识别
     */
    public boolean isRecognizing() {
        return isRecognizing;
    }

    /**
     * 释放资源
     */
    public void release() {
        cancelRecognition();
        audioRecorder = null;
        wsClient = null;
    }
}
