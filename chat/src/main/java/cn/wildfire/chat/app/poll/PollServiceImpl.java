/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.poll;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.net.Callback;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.poll.model.Poll;
import cn.wildfire.chat.kit.poll.model.PollOption;
import cn.wildfire.chat.kit.poll.model.PollVoterDetail;
import cn.wildfire.chat.kit.poll.service.PollService;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 投票服务实现类
 * <p>
 * 实现投票服务的网络请求逻辑，包括认证、数据解析等。
 * 参考 CollectionServiceImpl 实现。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class PollServiceImpl implements PollService {
    private static final String TAG = "PollServiceImpl";
    private static final String AUTH_CODE_ID = "poll";
    private static final int AUTH_CODE_TYPE = 2;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static PollServiceImpl instance;
    private String baseUrl;
    private OkHttpClient okHttpClient;

    private PollServiceImpl() {
        okHttpClient = new OkHttpClient.Builder()
            .build();
    }

    public static synchronized PollServiceImpl getInstance() {
        if (instance == null) {
            instance = new PollServiceImpl();
        }
        return instance;
    }

    /**
     * 设置投票服务基础URL
     *
     * @param baseUrl 基础URL，如 http://your-poll-server:8088
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
    public void createPoll(String groupId, String title, String description,
                          List<String> options, int visibility, int type,
                          int maxSelect, int anonymous, long endTime, int showResult,
                          final OnPollCallback<Poll> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "投票服务未配置");
            }
            return;
        }

        if (TextUtils.isEmpty(groupId) || TextUtils.isEmpty(title) || options == null || options.size() < 2) {
            if (callback != null) {
                callback.onError(-1, "参数错误");
            }
            return;
        }

        String url = baseUrl + "/api/polls";
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("title", title);
        if (!TextUtils.isEmpty(description)) {
            params.put("description", description);
        }
        params.put("options", new JSONArray(options));
        params.put("visibility", visibility);
        params.put("type", type);
        params.put("maxSelect", maxSelect);
        params.put("anonymous", anonymous);
        params.put("endTime", endTime);
        params.put("showResult", showResult);

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
                    Poll poll = Poll.fromJson(data);
                    callback.onSuccess(poll);
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
    public void getPoll(long pollId, final OnPollCallback<Poll> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "投票服务未配置");
            }
            return;
        }

        String url = baseUrl + "/api/polls/" + pollId;
        Map<String, Object> params = new HashMap<>();

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
                    Poll poll = Poll.fromJson(data);
                    callback.onSuccess(poll);
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
    public void vote(long pollId, List<Long> optionIds, final OnPollCallback<Void> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "投票服务未配置");
            }
            return;
        }

        if (optionIds == null || optionIds.isEmpty()) {
            if (callback != null) {
                callback.onError(-1, "请选择选项");
            }
            return;
        }

        String url = baseUrl + "/api/polls/" + pollId + "/vote";
        Map<String, Object> params = new HashMap<>();
        params.put("optionIds", new JSONArray(optionIds));

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
                callback.onSuccess(null);
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
    public void closePoll(long pollId, final OnPollCallback<Void> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "投票服务未配置");
            }
            return;
        }

        String url = baseUrl + "/api/polls/" + pollId + "/close";
        Map<String, Object> params = new HashMap<>();

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
                callback.onSuccess(null);
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
    public void deletePoll(long pollId, final OnPollCallback<Void> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "投票服务未配置");
            }
            return;
        }

        String url = baseUrl + "/api/polls/" + pollId + "/delete";
        Map<String, Object> params = new HashMap<>();

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
                callback.onSuccess(null);
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
    public void exportPollDetails(long pollId, final OnPollCallback<List<PollVoterDetail>> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "投票服务未配置");
            }
            return;
        }

        String url = baseUrl + "/api/polls/" + pollId + "/export";
        Map<String, Object> params = new HashMap<>();

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
                    JSONArray dataArray = result.optJSONArray("data");
                    List<PollVoterDetail> details = new ArrayList<>();
                    if (dataArray != null) {
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject detailJson = dataArray.getJSONObject(i);
                            PollVoterDetail detail = PollVoterDetail.fromJson(detailJson);
                            if (detail != null) {
                                details.add(detail);
                            }
                        }
                    }
                    callback.onSuccess(details);
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
    public void getMyPolls(final OnPollCallback<List<Poll>> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "投票服务未配置");
            }
            return;
        }

        // 后端API使用 POST /api/polls/my
        String url = baseUrl + "/api/polls/my";
        Map<String, Object> params = new HashMap<>();

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
                    JSONArray dataArray = result.optJSONArray("data");
                    List<Poll> polls = new ArrayList<>();
                    if (dataArray != null) {
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject pollJson = dataArray.getJSONObject(i);
                            Poll poll = Poll.fromJson(pollJson);
                            if (poll != null) {
                                polls.add(poll);
                            }
                        }
                    }
                    callback.onSuccess(polls);
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
     * 带认证的GET请求
     */
    private void getWithAuth(final String url, final Callback<JSONObject> callback) {
        // 先获取认证码
        String host = extractHost(baseUrl);
        ChatManager.Instance().getAuthCode(AUTH_CODE_ID, AUTH_CODE_TYPE, host, new GeneralCallback2() {
            @Override
            public void onSuccess(String authCode) {
                Request request = new Request.Builder()
                    .url(url)
                    .get()
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
