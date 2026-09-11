/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.asr;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * 实时语音识别 WebSocket 客户端
 * <p>
 * 连接 asr-api 的 /api/stream，由 asr-api 鉴权后转发给 wf-voice；内网测试时也可以直连 wf-voice。
 * 协议详见 wf-voice 项目的 docs/server-api.md：
 * <ol>
 *     <li>连接后发送的第一条文本消息是 clientId；需要边说边出字时，接着发送 partial</li>
 *     <li>二进制消息发送 16kHz、16-bit、单声道 PCM</li>
 *     <li>服务端每识别完一句，推送一条文本消息：[段开始毫秒时间戳+时长秒] 识别文本</li>
 *     <li>发送过 partial 时，说话过程中还会推送正在说的这句的中间结果：[PARTIAL] 识别文本</li>
 *     <li>说话结束时发送 eos，服务端推送完剩余识别结果后回复 [EOS]</li>
 * </ol>
 * 每次识别使用一个新实例。连接成功之前就可以发送音频，音频先缓存，连接成功后跟在 clientId 后面发送。
 * 所有回调都在主线程，调用 {@link #disconnect()} 之后不再回调。
 */
public class AsrWebSocketClient {
    private static final String TAG = "AsrWebSocketClient";

    private static final String MESSAGE_EOS = "eos";
    private static final String MESSAGE_PARTIAL = "partial";
    private static final String MESSAGE_EOS_ACK = "[EOS]";
    private static final String MESSAGE_PARTIAL_PREFIX = "[PARTIAL]";
    private static final String MESSAGE_PONG = "pong";
    private static final String MESSAGE_TRIAL_PREFIX = "[TRIAL]";

    // 发送 eos 前补发约 500ms 静音，让不支持 eos 的旧版本 wf-voice 也能通过 VAD 断句，识别出最后一句。
    // 静音和录音一样按 30ms（960 字节）一条消息发送，单条消息过大时 asr-api 或 wf-voice 会断开连接
    private static final int SILENCE_FRAME_BYTES = 960;
    private static final int SILENCE_PADDING_FRAMES = 17;

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Callback callback;

    // 下面的字段在录音线程、OkHttp 线程也会访问，读写时要持有 this 锁
    private WebSocket webSocket;
    // 连接成功之前发送的音频，连接成功后发送
    private final List<ByteString> pendingAudio = new ArrayList<>();
    private boolean opened;
    private boolean disconnected;

    /**
     * 回调接口（在主线程回调）
     */
    public interface Callback {
        /**
         * 连接成功，连接成功之前缓存的音频已经发送
         */
        void onConnected();

        /**
         * 正在说的这句的中间结果，之后会被新的中间结果或这句的最终结果替换
         * @param text 识别文本
         */
        void onPartialResult(@NonNull String text);

        /**
         * 识别出一句的最终结果
         * @param text 识别文本
         */
        void onResult(@NonNull String text);

        /**
         * eos 之前的识别结果已全部返回
         */
        void onEos();

        /**
         * 连接失败或连接被断开
         * @param error 错误信息
         */
        void onError(@NonNull String error);
    }

    public AsrWebSocketClient(@NonNull Callback callback) {
        this.callback = callback;
    }

    /**
     * 连接语音识别服务
     * @param url           WebSocket 地址，asr-api 或 wf-voice
     * @param clientId      客户端 ID，wf-voice 要求每个连接唯一
     * @param partialResult 是否边说边出字
     * @param authCode      连接 asr-api 时需要的认证码，直连 wf-voice 时传 null
     */
    public void connect(@NonNull String url, @NonNull String clientId, boolean partialResult, @Nullable String authCode) {
        Log.d(TAG, "正在连接语音识别服务: " + url);
        Request.Builder builder = new Request.Builder()
            .url(url);
        if (!TextUtils.isEmpty(authCode)) {
            builder.header(AsrAuth.HEADER_AUTH_CODE, authCode);
        }
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                onOpened(clientId, partialResult);
                postToMain(() -> callback.onConnected());
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "收到消息: " + text);
                handleMessage(text);
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket 已关闭: code=" + code + ", reason=" + reason);
                postToMain(() -> callback.onError("连接已断开"));
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                Log.e(TAG, "WebSocket 连接失败", t);
                String error = response != null && response.code() == 401 ? "语音识别服务鉴权失败" : "连接失败: " + t.getMessage();
                postToMain(() -> callback.onError(error));
            }
        };
        // 在锁内赋值，OkHttp 线程回调 onOpen 时 webSocket 一定已经赋值
        synchronized (this) {
            webSocket = OK_HTTP_CLIENT.newWebSocket(builder.build(), listener);
        }
    }

    /**
     * 连接成功，先发送 clientId 等指令，再发送连接成功之前缓存的音频
     */
    private synchronized void onOpened(@NonNull String clientId, boolean partialResult) {
        if (disconnected) {
            return;
        }
        webSocket.send(clientId);
        if (partialResult) {
            webSocket.send(MESSAGE_PARTIAL);
        }
        long pendingBytes = 0;
        for (ByteString data : pendingAudio) {
            webSocket.send(data);
            pendingBytes += data.size();
        }
        pendingAudio.clear();
        opened = true;
        // 16kHz、16-bit 的音频每毫秒 32 字节
        Log.d(TAG, "WebSocket 连接成功，发送连接前缓存的音频 " + pendingBytes / 32 + "ms");
    }

    /**
     * 发送音频数据，可以在任意线程调用。还没连接成功时先缓存，连接成功后发送
     * @param pcmData 16kHz、16-bit、单声道 PCM，会复制一份，调用方可以继续复用
     */
    public synchronized void sendAudioData(@NonNull byte[] pcmData) {
        if (disconnected) {
            return;
        }
        // ByteString.of 会复制数据
        ByteString data = ByteString.of(pcmData);
        if (!opened) {
            pendingAudio.add(data);
        } else if (!webSocket.send(data)) {
            Log.w(TAG, "发送音频数据失败");
        }
    }

    /**
     * 通知服务端说话结束，服务端返回剩余识别结果后回调 {@link Callback#onEos()}。需要在连接成功后调用
     */
    public synchronized void sendEos() {
        if (!opened || disconnected) {
            return;
        }
        byte[] silence = new byte[SILENCE_FRAME_BYTES];
        for (int i = 0; i < SILENCE_PADDING_FRAMES; i++) {
            webSocket.send(ByteString.of(silence));
        }
        webSocket.send(MESSAGE_EOS);
    }

    /**
     * 断开连接，之后不再回调
     */
    public synchronized void disconnect() {
        callback = null;
        disconnected = true;
        pendingAudio.clear();
        if (webSocket != null) {
            webSocket.close(1000, null);
            webSocket = null;
        }
    }

    private void handleMessage(@NonNull String message) {
        if (MESSAGE_EOS_ACK.equals(message)) {
            postToMain(() -> callback.onEos());
        } else if (message.startsWith(MESSAGE_PARTIAL_PREFIX)) {
            String text = message.substring(MESSAGE_PARTIAL_PREFIX.length()).trim();
            if (!text.isEmpty()) {
                postToMain(() -> callback.onPartialResult(text));
            }
        } else if (message.startsWith(MESSAGE_TRIAL_PREFIX)) {
            // 体验版每个连接只识别前 30 秒音频
            Log.w(TAG, message);
        } else if (!MESSAGE_PONG.equals(message)) {
            String text = parseResultText(message);
            if (!text.isEmpty()) {
                postToMain(() -> callback.onResult(text));
            }
        }
    }

    /**
     * 去掉识别结果的时间前缀，例如 "[1740992313000+2.35] 你好，世界，" 返回 "你好，世界，"
     */
    @NonNull
    private static String parseResultText(@NonNull String message) {
        if (message.startsWith("[")) {
            int end = message.indexOf(']');
            if (end > 0) {
                message = message.substring(end + 1);
            }
        }
        return message.trim();
    }

    /**
     * 切换到主线程执行回调，已断开连接时忽略
     */
    private void postToMain(@NonNull Runnable action) {
        mainHandler.post(() -> {
            if (callback != null) {
                action.run();
            }
        });
    }
}
