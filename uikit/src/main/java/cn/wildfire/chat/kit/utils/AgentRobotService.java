/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.wildfire.chat.kit.Config;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;

/**
 * 单聊 DSH（AI Agent）机器人识别。
 * <p>
 * 登录后向 ai-agent-server 查询当前 IM 用户已开通的机器人 id：
 * {@code POST {AI_AGENT_SERVER}/api/me/robot}，body {@code {"authCode":"<IM authCode>"}}，
 * 返回 {@code {"robotId":"robot_xxx"}}（未开通时为空串）。authCode 通过 IM SDK
 * {@code ChatManager.getAuthCode("admin", 2, host, ...)} 获取，host 为 AI 服务地址的 host。
 * </p>
 * <p>
 * 结果按登录用户缓存在 SharedPreferences 中（key 带 userId，账号切换不会串号），
 * 进程内再缓存一份；{@link #ensureLoaded(Callback)} 保证同一用户只请求一次，
 * {@link #refresh()} 强制重新查询（登录/连接成功后调用）。查询失败/未配置服务时
 * 一律按"无机器人"处理，不抛异常，不影响普通单聊。
 * </p>
 */
public class AgentRobotService {
    private static final String TAG = "AgentRobotService";
    private static final String PREFS_NAME = "wfc_agent_robot";
    private static final String KEY_ROBOT_ID_PREFIX = "robot_id_";
    private static final int TIMEOUT_MILLIS = 10000;

    /**
     * 机器人查询结果回调（主线程）。无机器人/未登录/服务未配置时 robotId 为空串。
     */
    public interface Callback {
        void onRobotId(String robotId);
    }

    private static final AgentRobotService INSTANCE = new AgentRobotService();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    /** 持续监听 robotId 变化的观察者（UI 收到结果后刷新状态/列表）。 */
    private final List<Callback> listeners = new CopyOnWriteArrayList<>();
    /** ensureLoaded 的一次性回调（结果到达后即清空）。 */
    private final List<Callback> pendingCallbacks = new CopyOnWriteArrayList<>();

    /** 当前用户已开通的机器人 id；空串=无。 */
    private volatile String robotId = "";
    /** robotId 对应的登录用户 id。 */
    private volatile String cacheUserId;
    /** 已发起过查询（成功/失败均算）的登录用户 id，避免重复请求。 */
    private volatile String attemptedUserId;
    private volatile boolean loading;
    private volatile String loadingUserId;

    private AgentRobotService() {
    }

    public static AgentRobotService Instance() {
        return INSTANCE;
    }

    /**
     * 当前登录用户的机器人 id（同步读取本地缓存）；未登录/无机器人/未配置服务返回空串。
     */
    public String getRobotId() {
        if (TextUtils.isEmpty(Config.AI_AGENT_SERVER)) {
            // 未配置 AI Agent 服务：关闭单聊 DSH 识别
            return "";
        }
        String userId = currentUserId();
        if (TextUtils.isEmpty(userId)) {
            return "";
        }
        synchronized (this) {
            if (!TextUtils.equals(userId, cacheUserId)) {
                // 首次读取或账号切换：只认当前用户自己的缓存，旧账号结果不串用
                cacheUserId = userId;
                robotId = readCached(userId);
                attemptedUserId = null;
            }
            return robotId == null ? "" : robotId;
        }
    }

    /**
     * 确保当前用户的机器人信息已尝试查询；已有结果（含"无机器人"）时立即回调，
     * 否则在后台查询完成后回调。回调只触发一次（主线程）。
     */
    public void ensureLoaded(Callback callback) {
        if (TextUtils.isEmpty(Config.AI_AGENT_SERVER)) {
            deliver(callback, "");
            return;
        }
        String userId = currentUserId();
        if (TextUtils.isEmpty(userId)) {
            deliver(callback, "");
            return;
        }
        boolean start = false;
        boolean known;
        String value;
        synchronized (this) {
            if (!TextUtils.equals(userId, cacheUserId)) {
                cacheUserId = userId;
                robotId = readCached(userId);
                attemptedUserId = null;
            }
            value = robotId == null ? "" : robotId;
            known = TextUtils.equals(userId, attemptedUserId);
            if (!known && !loading) {
                loading = true;
                loadingUserId = userId;
                start = true;
            }
        }
        if (start) {
            if (callback != null) {
                pendingCallbacks.add(callback);
            }
            startLoad(userId);
        } else if (known && !loading) {
            deliver(callback, value);
        } else if (callback != null) {
            // 正在查询：等本次结果到达后统一回调
            pendingCallbacks.add(callback);
        }
    }

    /**
     * 强制重新查询当前用户的机器人（登录/连接成功后调用；结果变化时通知观察者）。
     */
    public void refresh() {
        if (TextUtils.isEmpty(Config.AI_AGENT_SERVER)) {
            return;
        }
        String userId = currentUserId();
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        synchronized (this) {
            if (!TextUtils.equals(userId, cacheUserId)) {
                cacheUserId = userId;
                robotId = readCached(userId);
            }
            attemptedUserId = null;
            if (loading) {
                return;
            }
            loading = true;
            loadingUserId = userId;
        }
        startLoad(userId);
    }

    /** 注册持续观察者（UI 在机器人结果到达后刷新标题/列表）。 */
    public void addListener(Callback listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Callback listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * 账号退出/切换时清空内存与本地缓存（MainActivity 的登出分支调用）。
     */
    public void clear() {
        synchronized (this) {
            robotId = "";
            cacheUserId = null;
            attemptedUserId = null;
            loading = false;
            loadingUserId = null;
        }
        pendingCallbacks.clear();
        try {
            SharedPreferences prefs = prefs();
            if (prefs != null) {
                prefs.edit().clear().apply();
            }
        } catch (Exception e) {
            Log.w(TAG, "clear robot cache failed: " + e.getMessage());
        }
        mainHandler.post(() -> {
            for (Callback listener : listeners) {
                deliver(listener, "");
            }
        });
    }

    // ===================== 网络查询 =====================

    private void startLoad(String userId) {
        String server = Config.AI_AGENT_SERVER;
        if (TextUtils.isEmpty(server)) {
            // 未配置 AI Agent 服务：关闭单聊 DSH 识别
            finishLoad(userId, "");
            return;
        }
        server = server.trim();
        String host;
        try {
            host = new URL(server).getHost();
        } catch (Exception e) {
            Log.w(TAG, "invalid AI_AGENT_SERVER: " + e.getMessage());
            finishLoad(userId, "");
            return;
        }
        if (TextUtils.isEmpty(host)) {
            finishLoad(userId, "");
            return;
        }
        String base = server;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        final String baseUrl = base;
        try {
            ChatManager.Instance().getAuthCode("admin", 2, host, new GeneralCallback2() {
                @Override
                public void onSuccess(String result) {
                    if (TextUtils.isEmpty(result)) {
                        finishLoad(userId, "");
                        return;
                    }
                    new Thread(() -> {
                        String rid = requestRobotId(baseUrl, result);
                        finishLoad(userId, rid);
                    }, "agent-robot-lookup").start();
                }

                @Override
                public void onFail(int errorCode) {
                    Log.w(TAG, "getAuthCode failed, errorCode=" + errorCode);
                    finishLoad(userId, "");
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "getAuthCode exception: " + e.getMessage());
            finishLoad(userId, "");
        }
    }

    /**
     * POST {server}/api/me/robot，返回 robotId；网络/解析错误返回空串。
     * 在后台线程执行，不阻塞 UI。
     */
    private String requestRobotId(String server, String authCode) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(server + "/api/me/robot");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");

            JSONObject body = new JSONObject();
            body.put("authCode", authCode);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }

            int code = connection.getResponseCode();
            InputStream input = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
            String text = readAll(input);
            if (code < 200 || code >= 300) {
                Log.w(TAG, "lookup robot failed, http " + code);
                return "";
            }
            if (TextUtils.isEmpty(text)) {
                return "";
            }
            return new JSONObject(text).optString("robotId", "");
        } catch (Exception e) {
            Log.w(TAG, "lookup robot failed: " + e.getMessage());
            return "";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readAll(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        try (InputStream in = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return output.toString("UTF-8");
        }
    }

    // ===================== 结果落地 / 通知 =====================

    private void finishLoad(String userId, String rid) {
        mainHandler.post(() -> {
            String current = currentUserId();
            if (!TextUtils.equals(userId, current)) {
                // 查询期间账号已切换：丢弃旧账号结果
                synchronized (this) {
                    if (TextUtils.equals(loadingUserId, userId)) {
                        loading = false;
                        loadingUserId = null;
                    }
                }
                return;
            }
            String value = rid == null ? "" : rid;
            synchronized (this) {
                loading = false;
                loadingUserId = null;
                cacheUserId = userId;
                attemptedUserId = userId;
                robotId = value;
            }
            if (TextUtils.isEmpty(value)) {
                removeCached(userId);
            } else {
                writeCached(userId, value);
            }

            List<Callback> callbacks = new ArrayList<>(pendingCallbacks);
            pendingCallbacks.clear();
            for (Callback callback : callbacks) {
                deliver(callback, value);
            }
            for (Callback listener : listeners) {
                deliver(listener, value);
            }
        });
    }

    private void deliver(Callback callback, String value) {
        if (callback == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.onRobotId(value);
        } else {
            mainHandler.post(() -> callback.onRobotId(value));
        }
    }

    // ===================== 本地缓存（按登录用户隔离） =====================

    private SharedPreferences prefs() {
        Context context = ChatManager.Instance().getApplicationContext();
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private String readCached(String userId) {
        try {
            SharedPreferences prefs = prefs();
            if (prefs == null) {
                return "";
            }
            return prefs.getString(KEY_ROBOT_ID_PREFIX + userId, "");
        } catch (Exception e) {
            return "";
        }
    }

    private void writeCached(String userId, String value) {
        try {
            SharedPreferences prefs = prefs();
            if (prefs != null) {
                prefs.edit().putString(KEY_ROBOT_ID_PREFIX + userId, value).apply();
            }
        } catch (Exception e) {
            Log.w(TAG, "persist robot id failed: " + e.getMessage());
        }
    }

    private void removeCached(String userId) {
        try {
            SharedPreferences prefs = prefs();
            if (prefs != null) {
                prefs.edit().remove(KEY_ROBOT_ID_PREFIX + userId).apply();
            }
        } catch (Exception e) {
            Log.w(TAG, "remove robot id failed: " + e.getMessage());
        }
    }

    private String currentUserId() {
        try {
            return ChatManager.Instance().getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
