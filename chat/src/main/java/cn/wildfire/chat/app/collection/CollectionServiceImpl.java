/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.collection;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.kit.collection.CollectionService;
import cn.wildfire.chat.kit.collection.model.Collection;
import cn.wildfire.chat.kit.net.Callback;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 接龙服务实现类
 * <p>
 * 实现接龙服务的网络请求逻辑，包括认证、数据解析等。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class CollectionServiceImpl implements CollectionService {
    private static final String TAG = "CollectionServiceImpl";
    private static final String AUTH_CODE_ID = "collection";
    private static final int AUTH_CODE_TYPE = 2;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static CollectionServiceImpl instance;
    private String baseUrl;
    private OkHttpClient okHttpClient;

    private CollectionServiceImpl() {
        okHttpClient = new OkHttpClient.Builder()
            .build();
    }

    public static synchronized CollectionServiceImpl getInstance() {
        if (instance == null) {
            instance = new CollectionServiceImpl();
        }
        return instance;
    }

    /**
     * 设置接龙服务基础URL
     *
     * @param baseUrl 基础URL，如 https://jielong.wildfirechat.net
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
    public void createCollection(String groupId, String title, String desc, String template,
                                int expireType, long expireAt, int maxParticipants,
                                final CreateCollectionCallback callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "接龙服务未配置");
            }
            return;
        }

        if (TextUtils.isEmpty(groupId) || TextUtils.isEmpty(title)) {
            if (callback != null) {
                callback.onError(-1, "参数错误");
            }
            return;
        }

        String url = baseUrl + "/api/collections";
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("title", title);
        if (!TextUtils.isEmpty(desc)) {
            params.put("description", desc);
        }
        if (!TextUtils.isEmpty(template)) {
            params.put("template", template);
        }
        params.put("expireType", expireType);
        if (expireType == 1) {
            params.put("expireAt", expireAt);
        }
        params.put("maxParticipants", maxParticipants);

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
                    Collection collection = Collection.fromJson(data);
                    callback.onSuccess(collection);
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
    public void getCollection(long collectionId, String groupId, final GetCollectionCallback callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "接龙服务未配置");
            }
            return;
        }

        String url = baseUrl + "/api/collections/" + collectionId + "/detail";
        Map<String, Object> params = new HashMap<>();
        if (!TextUtils.isEmpty(groupId)) {
            params.put("groupId", groupId);
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
                    Collection collection = Collection.fromJson(data);
                    callback.onSuccess(collection);
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
    public void joinCollection(long collectionId, String groupId, String content, final OperationCallback callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "接龙服务未配置");
            }
            return;
        }

        if (TextUtils.isEmpty(content)) {
            if (callback != null) {
                callback.onError(-1, "参与内容不能为空");
            }
            return;
        }

        String url = baseUrl + "/api/collections/" + collectionId + "/join";
        Map<String, Object> params = new HashMap<>();
        if (!TextUtils.isEmpty(groupId)) {
            params.put("groupId", groupId);
        }
        params.put("content", content);

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
                int code = result.optInt("code", -1);
                if (code != 0) {
                    callback.onError(code, result.optString("message", "未知错误"));
                    return;
                }
                callback.onSuccess();
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
    public void deleteCollectionEntry(long collectionId, String groupId, final OperationCallback callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "接龙服务未配置");
            }
            return;
        }

        String url = baseUrl + "/api/collections/" + collectionId + "/delete";
        Map<String, Object> params = new HashMap<>();
        if (!TextUtils.isEmpty(groupId)) {
            params.put("groupId", groupId);
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
                int code = result.optInt("code", -1);
                if (code != 0) {
                    callback.onError(code, result.optString("message", "未知错误"));
                    return;
                }
                callback.onSuccess();
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
    public void closeCollection(long collectionId, String groupId, final OperationCallback callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "接龙服务未配置");
            }
            return;
        }

        String url = baseUrl + "/api/collections/" + collectionId + "/close";
        Map<String, Object> params = new HashMap<>();
        if (!TextUtils.isEmpty(groupId)) {
            params.put("groupId", groupId);
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
                int code = result.optInt("code", -1);
                if (code != 0) {
                    callback.onError(code, result.optString("message", "未知错误"));
                    return;
                }
                callback.onSuccess();
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
     * 带认证的POST请求
     * <p>
     * authCode通过HTTP Header传递，header名称为"authCode"
     *
     * @param url      请求URL
     * @param params   请求参数
     * @param callback 回调
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
                            jsonBody.put(entry.getKey(), entry.getValue());
                        } catch (JSONException e) {
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
                        } catch (JSONException e) {
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
     *
     * @param url URL
     * @return host
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
