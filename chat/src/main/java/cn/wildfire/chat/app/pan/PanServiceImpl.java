/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.pan;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.pan.api.PanService;
import cn.wildfire.chat.kit.pan.model.CreateFileRequest;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanSpace;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 网盘服务实现类
 * <p>
 * 实现网盘服务的网络请求逻辑，包括认证、数据解析等。
 * 参考 PollServiceImpl 实现。
 * </p>
 */
public class PanServiceImpl implements PanService {
    private static final String TAG = "PanServiceImpl";
    private static final String AUTH_CODE_ID = "admin";
    private static final int AUTH_CODE_TYPE = 2;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static PanServiceImpl instance;
    private String baseUrl;
    private OkHttpClient okHttpClient;

    private PanServiceImpl() {
        okHttpClient = new OkHttpClient.Builder()
            .build();
    }

    public static synchronized PanServiceImpl getInstance() {
        if (instance == null) {
            instance = new PanServiceImpl();
        }
        return instance;
    }

    /**
     * 设置网盘服务基础URL
     *
     * @param baseUrl 基础URL，如 http://localhost:8081
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
    public void getSpaces(final Callback<List<PanSpace>> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/spaces/list";
        postWithAuth(url, new HashMap<>(), new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                if (callback == null) return;
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
                    JSONArray data = result.optJSONArray("data");
                    if (data == null) {
                        callback.onError(-1, "返回数据为空");
                        return;
                    }
                    List<PanSpace> list = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.optJSONObject(i);
                        if (obj != null) {
                            list.add(PanSpace.fromJson(obj));
                        }
                    }
                    callback.onSuccess(list);
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
    public void getUserPublicSpace(String userId, final Callback<PanSpace> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/spaces/user/public";
        Map<String, Object> params = new HashMap<>();
        params.put("targetUserId", userId);
        android.util.Log.d(TAG, "getUserPublicSpace: url=" + url + ", targetUserId=" + userId);
        
        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                android.util.Log.d(TAG, "getUserPublicSpace response: " + (result != null ? result.toString() : "null"));
                if (callback == null) return;
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
                    callback.onSuccess(PanSpace.fromJson(data));
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
    public void getFiles(Long spaceId, Long parentId, final Callback<List<PanFile>> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/spaces/files";
        Map<String, Object> params = new HashMap<>();
        params.put("spaceId", spaceId);
        if (parentId != null && parentId > 0) {
            params.put("parentId", parentId);
        } else {
            params.put("parentId", 0);
        }
        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                if (callback == null) return;
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
                    JSONArray data = result.optJSONArray("data");
                    if (data == null) {
                        callback.onError(-1, "返回数据为空");
                        return;
                    }
                    List<PanFile> list = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.optJSONObject(i);
                        if (obj != null) {
                            list.add(PanFile.fromJson(obj));
                        }
                    }
                    callback.onSuccess(list);
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
    public void createFolder(Long spaceId, Long parentId, String name, final Callback<PanFile> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/files/folder";
        Map<String, Object> params = new HashMap<>();
        params.put("spaceId", spaceId);
        if (parentId != null) {
            params.put("parentId", parentId);
        }
        params.put("name", name);

        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                handleObjectResult(result, callback);
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
    public void createFile(CreateFileRequest request, final Callback<PanFile> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/files";
        Map<String, Object> params = new HashMap<>();
        params.put("spaceId", request.getSpaceId());
        if (request.getParentId() != null) {
            params.put("parentId", request.getParentId());
        }
        params.put("name", request.getName());
        params.put("size", request.getSize());
        params.put("mimeType", request.getMimeType());
        params.put("md5", request.getMd5() != null ? request.getMd5() : "");
        params.put("storageUrl", request.getStorageUrl());
        params.put("copy", request.getCopy() != null ? request.getCopy() : false);

        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                handleObjectResult(result, callback);
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
    public void deleteFile(Long fileId, final PanService.SimpleCallback callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/files/delete";
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", fileId);
        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                handleSimpleResult(result, callback);
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
    public void moveFile(Long fileId, Long targetSpaceId, Long targetParentId, final Callback<PanFile> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/files/move";
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", fileId);
        params.put("targetSpaceId", targetSpaceId);
        if (targetParentId != null && targetParentId > 0) {
            params.put("targetParentId", targetParentId);
        } else {
            params.put("targetParentId", 0);
        }

        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                handleObjectResult(result, callback);
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
    public void copyFile(Long fileId, Long targetSpaceId, Long targetParentId, boolean copy, final Callback<PanFile> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/files/copy";
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", fileId);
        params.put("targetSpaceId", targetSpaceId);
        if (targetParentId != null && targetParentId > 0) {
            params.put("targetParentId", targetParentId);
        } else {
            params.put("targetParentId", 0);
        }

        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                handleObjectResult(result, callback);
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
    public void renameFile(Long fileId, String newName, final Callback<PanFile> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/files/rename";
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", fileId);
        params.put("newName", newName);

        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                handleObjectResult(result, callback);
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
    public void getFileUrl(Long fileId, final Callback<String> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/files/url";
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", fileId);

        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                if (callback == null) return;
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
                    String url = data.optString("storageUrl");
                    callback.onSuccess(url);
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
    public void checkSpaceWritePermission(Long spaceId, final Callback<Boolean> callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/files/check-permission";
        Map<String, Object> params = new HashMap<>();
        params.put("spaceId", spaceId);

        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                if (callback == null) return;
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
                    // 返回的是 boolean 值
                    boolean hasPermission = result.optBoolean("data", false);
                    callback.onSuccess(hasPermission);
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
    public void checkUploadPermission(Long spaceId, Callback<Boolean> callback) {
        // 与 checkSpaceWritePermission 相同
        checkSpaceWritePermission(spaceId, callback);
    }

    @Override
    public void duplicateFile(Long fileId, Long targetSpaceId, Long targetParentId, final SimpleCallback callback) {
        if (!checkConfigured(callback)) return;

        String url = baseUrl + "/api/v1/files/duplicate";
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", fileId);
        params.put("targetSpaceId", targetSpaceId);
        if (targetParentId != null && targetParentId > 0) {
            params.put("targetParentId", targetParentId);
        } else {
            params.put("targetParentId", 0);
        }

        postWithAuth(url, params, new cn.wildfire.chat.kit.net.SimpleCallback<JSONObject>() {
            @Override
            public void onUiSuccess(JSONObject result) {
                handleSimpleResult(result, callback);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (callback != null) {
                    callback.onError(code, msg);
                }
            }
        });
    }

    // ============== 私有方法 ==============

    private boolean checkConfigured(Callback<?> callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "网盘服务未配置");
            }
            return false;
        }
        return true;
    }

    private boolean checkConfigured(PanService.SimpleCallback callback) {
        if (!isConfigured()) {
            if (callback != null) {
                callback.onError(-1, "网盘服务未配置");
            }
            return false;
        }
        return true;
    }

    private void handleObjectResult(JSONObject result, Callback<PanFile> callback) {
        if (callback == null) return;
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
            callback.onSuccess(PanFile.fromJson(data));
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    private void handleSimpleResult(JSONObject result, PanService.SimpleCallback callback) {
        if (callback == null) return;
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
            callback.onSuccess();
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    /**
     * 带认证的GET请求
     */
    private void getWithAuth(final String url, final cn.wildfire.chat.kit.net.SimpleCallback<JSONObject> callback) {
        ChatManager.Instance().getAuthCode(AUTH_CODE_ID, AUTH_CODE_TYPE, Config.IM_SERVER_HOST, new GeneralCallback2() {
            @Override
            public void onSuccess(String authCode) {
                Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("authCode", authCode)
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
                        if (callback == null) return;
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
     * 带认证的POST请求
     */
    private void postWithAuth(final String url, final Map<String, Object> params, final cn.wildfire.chat.kit.net.SimpleCallback<JSONObject> callback) {
        ChatManager.Instance().getAuthCode(AUTH_CODE_ID, AUTH_CODE_TYPE, Config.IM_SERVER_HOST, new GeneralCallback2() {
            @Override
            public void onSuccess(String authCode) {
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
                        if (callback == null) return;
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

    private String extractHost(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        String host = url;
        if (host.startsWith("https://")) {
            host = host.substring(8);
        } else if (host.startsWith("http://")) {
            host = host.substring(7);
        }
        int slashIndex = host.indexOf('/');
        if (slashIndex > 0) {
            host = host.substring(0, slashIndex);
        }
        return host;
    }
}
