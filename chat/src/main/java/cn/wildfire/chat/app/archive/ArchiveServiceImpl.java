/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.archive;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.app.archive.model.ArchivedMessage;
import cn.wildfire.chat.app.archive.model.ArchiveMessagePayload;
import cn.wildfire.chat.kit.net.Callback;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.archive.service.ArchiveService;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 归档服务实现类
 * <p>
 * 实现归档服务的网络请求逻辑，包括认证、数据解析、消息转换等。
 * 参考 PollServiceImpl 实现。
 * </p>
 *
 * @author WildFireChat
 * @since 2025
 */
public class ArchiveServiceImpl implements ArchiveService {
    private static final String TAG = "ArchiveServiceImpl";
    private static final String AUTH_CODE_ID = "admin";
    private static final int AUTH_CODE_TYPE = 2;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static ArchiveServiceImpl instance;
    private String baseUrl;
    private OkHttpClient okHttpClient;
    private long messageIdCounter;

    private ArchiveServiceImpl() {
        okHttpClient = new OkHttpClient.Builder()
                .build();
        messageIdCounter = 0;
    }

    public static synchronized ArchiveServiceImpl getInstance() {
        if (instance == null) {
            instance = new ArchiveServiceImpl();
        }
        return instance;
    }

    /**
     * 设置归档服务基础URL
     *
     * @param baseUrl 基础URL，如 http://your-archive-server:8088
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 检查服务是否已配置
     *
     * @return true=已配置
     */
    public boolean isConfigured() {
        return !TextUtils.isEmpty(baseUrl);
    }

    @Override
    public void getArchivedMessages(int conversationType, String convTarget, long startMid,
                                    boolean before, int limit, final OnArchiveCallback<ArchiveMessageResult> callback) {
        getArchivedMessages(conversationType, convTarget, 0, startMid, before, limit, callback);
    }

    @Override
    public void getArchivedMessages(int conversationType, String convTarget, int convLine, long startMid,
                                    boolean before, int limit, final OnArchiveCallback<ArchiveMessageResult> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "归档服务未配置");
            }
            return;
        }

        if (TextUtils.isEmpty(convTarget)) {
            if (callback != null) {
                callback.onError(-1, "会话目标不能为空");
            }
            return;
        }

        String url = baseUrl + "/api/messages/fetch";
        Map<String, Object> params = new HashMap<>();
        params.put("convType", conversationType);
        params.put("convTarget", convTarget);
        params.put("convLine", convLine);
        params.put("before", before);
        params.put("limit", Math.min(limit, 100));
        params.put("startMid", startMid);

        postWithAuth(url, params, new SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                if (callback == null) {
                    return;
                }
                if (result == null) {
                    callback.onError(-1, "返回数据为空");
                    return;
                }
                try {
                    int code = result.optInt("code", -1);
                    if (code != 0) {
                        callback.onError(code, result.optString("message", "未知错误"));
                        return;
                    }
                    JSONObject data = result.optJSONObject("data");
                    if (data == null) {
                        callback.onError(-1, "返回数据为空");
                        return;
                    }
                    ArchiveMessageResult archiveResult = parseResultFromJson(data);
                    callback.onSuccess(archiveResult);
                } catch (Exception e) {
                    callback.onError(-1, e.getMessage());
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (callback != null) {
                    callback.onError(code, msg);
                }
            }
        });
    }

    @Override
    public void searchArchivedMessages(String keyword, int limit, final OnArchiveCallback<ArchiveMessageResult> callback) {
        searchArchivedMessages(keyword, null, null, null, 0, true, limit, callback);
    }

    @Override
    public void searchArchivedMessages(String keyword, Integer conversationType, String convTarget,
                                       Integer convLine, long startMid, boolean before, int limit,
                                       final OnArchiveCallback<ArchiveMessageResult> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "归档服务未配置");
            }
            return;
        }

        if (TextUtils.isEmpty(keyword)) {
            if (callback != null) {
                callback.onError(-1, "搜索关键词不能为空");
            }
            return;
        }

        String url = baseUrl + "/api/messages/search";
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("convTarget", convTarget != null ? convTarget : "");
        params.put("before", before);
        params.put("limit", Math.min(limit, 100));
        params.put("startMid", startMid);

        // 可选参数
        if (conversationType != null) {
            params.put("convType", conversationType);
        }
        if (convLine != null) {
            params.put("convLine", convLine);
        }

        postWithAuth(url, params, new SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                if (callback == null) {
                    return;
                }
                if (result == null) {
                    callback.onError(-1, "返回数据为空");
                    return;
                }
                try {
                    int code = result.optInt("code", -1);
                    if (code != 0) {
                        callback.onError(code, result.optString("message", "未知错误"));
                        return;
                    }
                    JSONObject data = result.optJSONObject("data");
                    if (data == null) {
                        callback.onError(-1, "返回数据为空");
                        return;
                    }
                    ArchiveMessageResult archiveResult = parseResultFromJson(data);
                    callback.onSuccess(archiveResult);
                } catch (Exception e) {
                    callback.onError(-1, e.getMessage());
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (callback != null) {
                    callback.onError(code, msg);
                }
            }
        });
    }

    /**
     * 从JSON解析归档消息结果
     */
    private ArchiveMessageResult parseResultFromJson(JSONObject json) {
        if (json == null) {
            return new ArchiveMessageResult(new ArrayList<>(), false, 0);
        }

        boolean hasMore = json.optBoolean("hasMore", false);
        long nextStartMid = json.isNull("nextStartMid") ? 0 : json.optLong("nextStartMid", 0);

        List<Message> messages = new ArrayList<>();
        JSONArray messagesArray = json.optJSONArray("messages");
        if (messagesArray != null) {
            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject messageJson = messagesArray.optJSONObject(i);
                if (messageJson != null) {
                    Message message = parseArchivedMessage(messageJson);
                    if (message != null) {
                        messages.add(message);
                    }
                }
            }
        }

        return new ArchiveMessageResult(messages, hasMore, nextStartMid);
    }

    /**
     * 解析归档消息JSON为Message对象
     */
    private Message parseArchivedMessage(JSONObject json) {
        if (json == null) {
            return null;
        }

        // 解析为 ArchivedMessage 对象
        ArchivedMessage archivedMsg = ArchivedMessage.fromJson(json);
        if (archivedMsg == null) {
            return null;
        }

        return convertArchivedMessageToMessage(archivedMsg);
    }

    /**
     * 将归档消息转换为SDK的Message对象
     */
    private Message convertArchivedMessageToMessage(ArchivedMessage archivedMsg) {
        if (archivedMsg == null) {
            return null;
        }

        Message message = new Message();
        // 从远程同步回来的消息，没有消息ID，使用递减的负数确保唯一
        messageIdCounter--;
        message.messageId = messageIdCounter;
        message.messageUid = archivedMsg.mid;
        message.sender = archivedMsg.senderId;

        // 判断消息方向：如果是当前用户发送的就是发送的，否则就是收到的
        String currentUserId = ChatManager.Instance().getUserId();
        if (currentUserId != null && currentUserId.equals(archivedMsg.senderId)) {
            message.direction = MessageDirection.Send;
            // 发送的消息状态设为发送成功
            message.status = MessageStatus.Sent;
        } else {
            message.direction = MessageDirection.Receive;
            // 接收的消息状态设为已读
            message.status = MessageStatus.Readed;
        }

        // 设置会话信息
        Conversation conversation = new Conversation();
        conversation.type = Conversation.ConversationType.type(archivedMsg.convType);
        conversation.target = archivedMsg.convTarget;
        conversation.line = archivedMsg.convLine;
        message.conversation = conversation;

        // 转换时间为毫秒时间戳
        Date msgDate = archivedMsg.getLocalMessageDate();
        message.serverTime = msgDate.getTime();

        // 使用 payload 创建 SDK 的 MessagePayload
        MessagePayload sdkPayload;
        if (archivedMsg.payload != null) {
            sdkPayload = archivedMsg.payload.toSDKPayload(archivedMsg.contentType);
        } else {
            // 如果没有 payload，创建一个空的 payload
            sdkPayload = new MessagePayload();
            sdkPayload.type = archivedMsg.contentType;
        }

        // 使用 ChatManager 解析消息内容
        MessageContent content = ChatManager.Instance().messageContentFromPayload(sdkPayload, message.sender);
        if (content == null) {
            // 如果解析失败，使用未知消息内容类型
            content = new cn.wildfirechat.message.UnknownMessageContent();
        }
        message.content = content;

        return message;
    }

    /**
     * 带认证的POST请求
     */
    private void postWithAuth(final String url, final Map<String, Object> params, final Callback<JSONObject> callback) {
        // 先获取认证码
        String host = extractHost(baseUrl);
        ChatManager.Instance().getAuthCode(AUTH_CODE_ID, AUTH_CODE_TYPE, host, new GeneralCallback2() {
            @Override
            public void onSuccess(String authCode) {
                // 构建JSON请求体
                JSONObject jsonBody = new JSONObject();
                if (params != null) {
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        try {
                            if (entry.getValue() != null) {
                                jsonBody.put(entry.getKey(), entry.getValue());
                            }
                        } catch (org.json.JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                RequestBody body = RequestBody.create(JSON, jsonBody.toString());
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("authCode", authCode)
                        .addHeader("Content-Type", "application/json")
                        .build();

                okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (callback != null) {
                            callback.onFailure(-1, e.getMessage());
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (callback == null) {
                            return;
                        }
                        if (!response.isSuccessful()) {
                            callback.onFailure(response.code(), response.message());
                            return;
                        }
                        try {
                            String result = response.body().string();
                            JSONObject jsonObject = new JSONObject(result);
                            callback.onSuccess(jsonObject);
                        } catch (org.json.JSONException e) {
                            callback.onFailure(-1, "JSON解析错误: " + e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onFail(int errorCode) {
                if (callback != null) {
                    callback.onFailure(errorCode, "获取认证码失败");
                }
            }
        });
    }

    /**
     * 从URL中提取host
     */
    private String extractHost(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        String host = url;
        // 去除协议前缀
        if (host.startsWith("https://")) {
            host = host.substring(8);
        } else if (host.startsWith("http://")) {
            host = host.substring(7);
        }
        // 去除路径部分
        int slashIndex = host.indexOf('/');
        if (slashIndex > 0) {
            host = host.substring(0, slashIndex);
        }
        return host;
    }
}
