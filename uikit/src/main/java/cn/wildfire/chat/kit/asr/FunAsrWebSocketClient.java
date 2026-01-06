/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.asr;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * FunASR WebSocket 客户端
 * 负责与 FunASR 服务器进行 WebSocket 通信
 */
public class FunAsrWebSocketClient {
    private static final String TAG = "FunAsrWebSocketClient";
    private static final String SERVER_URL = "ws://192.168.1.33:10096";

    private OkHttpClient client;
    private WebSocket webSocket;
    private Handler mainHandler;
    private AsrResponseCallback callback;
    private boolean isConnected = false;

    /**
     * 连接状态枚举
     */
    public enum State {
        DISCONNECTED,   // 未连接
        CONNECTING,     // 连接中
        CONNECTED,      // 已连接
        ERROR           // 错误
    }

    /**
     * ASR 响应回调接口
     */
    public interface AsrResponseCallback {
        /**
         * 连接状态变化（在主线程回调）
         * @param state 连接状态
         */
        void onConnectionStateChanged(State state);

        /**
         * 识别结果（在主线程回调）
         * @param text 识别的文本
         * @param isFinal 是否为最终结果
         * @param mode 识别模式 (2pass-offline/online/offline)
         */
        void onRecognitionResult(String text, boolean isFinal, String mode);

        /**
         * 错误（在主线程回调）
         * @param error 错误信息
         */
        void onError(String error);
    }

    public FunAsrWebSocketClient() {
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 连接到 FunASR 服务器
     * @param callback 回调接口
     */
    public void connect(@NonNull AsrResponseCallback callback) {
        this.callback = callback;

        if (isConnected && webSocket != null) {
            Log.w(TAG, "WebSocket 已经连接，无需重复连接");
            return;
        }

        // 创建 OkHttpClient，设置无限读取超时（因为语音识别可能持续时间较长）
        client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)  // 发送心跳包保持连接
            .build();

        Request request = new Request.Builder()
            .url(SERVER_URL)
            .build();

        // 创建 WebSocket 连接
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket 连接成功");
                isConnected = true;
                notifyConnectionStateChanged(State.CONNECTED);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "收到文本消息: " + text);
                handleJsonMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                // FunASR 使用文本格式的 JSON，不使用二进制消息
                Log.d(TAG, "收到二进制消息，长度: " + bytes.size());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 正在关闭: code=" + code + ", reason=" + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 已关闭: code=" + code + ", reason=" + reason);
                isConnected = false;
                notifyConnectionStateChanged(State.DISCONNECTED);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket 连接失败", t);
                isConnected = false;
                notifyError("连接失败: " + t.getMessage());
                notifyConnectionStateChanged(State.ERROR);
            }
        });

        notifyConnectionStateChanged(State.CONNECTING);
        Log.d(TAG, "正在连接到 FunASR 服务器: " + SERVER_URL);
    }

    /**
     * 发送初始化配置
     * @param mode 识别模式 (2pass/online/offline)
     * @param hotwords 热词配置（可选），格式为 JSON 字符串：{"word": weight}
     *                 例如：{"Over": 50} 表示 "Over" 的权重为 50
     */
    public void sendInitConfig(@NonNull String mode, @Nullable String hotwords) {
        if (!isConnected || webSocket == null) {
            Log.e(TAG, "WebSocket 未连接，无法发送配置");
            return;
        }

        try {
            JSONObject config = new JSONObject();
            config.put("chunk_size", new JSONArray(new int[]{5, 10, 5}));
            config.put("wav_name", "android");
            config.put("is_speaking", true);
            config.put("chunk_interval", 10);
            config.put("itn", false);
            config.put("mode", mode);

            // 添加热词配置
            if (hotwords != null && !hotwords.isEmpty()) {
                config.put("hotwords", hotwords);
                Log.d(TAG, "添加热词配置: " + hotwords);
            }

            String configStr = config.toString();
            boolean sent = webSocket.send(configStr);
            Log.d(TAG, "发送初始化配置: " + configStr + ", 发送结果: " + sent);
        } catch (JSONException e) {
            Log.e(TAG, "构造初始化配置失败", e);
            notifyError("构造配置失败: " + e.getMessage());
        }
    }

    /**
     * 发送初始化配置（无热词）
     * @param mode 识别模式 (2pass/online/offline)
     */
    public void sendInitConfig(@NonNull String mode) {
        sendInitConfig(mode, null);
    }

    /**
     * 发送音频数据（PCM 格式）
     * @param pcmData PCM 音频数据（16-bit, 16kHz, 单声道）
     */
    public void sendAudioData(@NonNull byte[] pcmData) {
        if (!isConnected || webSocket == null) {
            Log.w(TAG, "WebSocket 未连接，跳过发送音频数据");
            return;
        }

        // 使用 ByteString 发送二进制数据
        ByteString byteString = ByteString.of(pcmData);
        boolean sent = webSocket.send(byteString);
        if (!sent) {
            Log.w(TAG, "发送音频数据失败");
        }
    }

    /**
     * 发送停止消息（告诉服务器说话结束）
     */
    public void sendStopMessage() {
        if (!isConnected || webSocket == null) {
            Log.e(TAG, "WebSocket 未连接，无法发送停止消息");
            return;
        }

        try {
            JSONObject stopMsg = new JSONObject();
            stopMsg.put("chunk_size", new JSONArray(new int[]{5, 10, 5}));
            stopMsg.put("wav_name", "android");
            stopMsg.put("is_speaking", false);  // 标识说话结束
            stopMsg.put("chunk_interval", 10);

            String stopMsgStr = stopMsg.toString();
            boolean sent = webSocket.send(stopMsgStr);
            Log.d(TAG, "发送停止消息: " + stopMsgStr + ", 发送结果: " + sent);
        } catch (JSONException e) {
            Log.e(TAG, "构造停止消息失败", e);
            notifyError("构造停止消息失败: " + e.getMessage());
        }
    }

    /**
     * 断开连接
     * @param code 关闭代码
     * @param reason 关闭原因
     */
    public void disconnect(int code, String reason) {
        Log.d(TAG, "主动断开连接: code=" + code + ", reason=" + reason);

        if (webSocket != null) {
            webSocket.close(code, reason);
            webSocket = null;
        }

        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            client = null;
        }

        isConnected = false;
    }

    /**
     * 处理服务器返回的 JSON 消息
     * @param text JSON 字符串
     */
    private void handleJsonMessage(@NonNull String text) {
        try {
            JSONObject json = new JSONObject(text);
            String resultText = json.optString("text", "");
            boolean isFinal = json.optBoolean("is_final", false);
            String mode = json.optString("mode", "");

            Log.d(TAG, "识别结果: text=" + resultText + ", isFinal=" + isFinal + ", mode=" + mode);

            notifyRecognitionResult(resultText, isFinal, mode);
        } catch (JSONException e) {
            Log.e(TAG, "解析识别结果失败", e);
            notifyError("解析结果失败: " + e.getMessage());
        }
    }

    /**
     * 通知连接状态变化（切换到主线程）
     * @param state 连接状态
     */
    private void notifyConnectionStateChanged(@NonNull State state) {
        if (callback != null) {
            mainHandler.post(() -> callback.onConnectionStateChanged(state));
        }
    }

    /**
     * 通知识别结果（切换到主线程）
     * @param text 识别文本
     * @param isFinal 是否最终结果
     * @param mode 识别模式
     */
    private void notifyRecognitionResult(@NonNull String text, boolean isFinal, @NonNull String mode) {
        if (callback != null) {
            mainHandler.post(() -> callback.onRecognitionResult(text, isFinal, mode));
        }
    }

    /**
     * 通知错误（切换到主线程）
     * @param error 错误信息
     */
    private void notifyError(@NonNull String error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(error));
        }
    }

    /**
     * 获取连接状态
     * @return 是否已连接
     */
    public boolean isConnected() {
        return isConnected;
    }
}
