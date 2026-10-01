/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.pan;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.pan.PanService;
import cn.wildfire.chat.kit.pan.model.PanDocEntry;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanFileUrl;
import cn.wildfire.chat.kit.pan.model.PanShare;
import cn.wildfire.chat.kit.pan.model.PanSpace;
import cn.wildfire.chat.kit.pan.model.PanUploadCancelToken;
import cn.wildfire.chat.kit.pan.model.PanVersion;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;
import cn.wildfirechat.remote.UploadMediaCallback;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 网盘服务实现：对接<strong>独立版</strong> wf-pan-server。
 * <p>
 * {@code PAN_SERVER + /api/v1/**}；每个请求先用 IM 的 authCode
 * （{@code getAuthCode("admin", 2, host)}）换取授权码，放在 HTTP 头 {@code authCode} 里；
 * 响应统一是 {@code {code,message,data}}，{@code code != 0} 视为业务错误。
 * 上传复用 IM SDK 的 {@code ChatManager.uploadMediaFile}。
 */
public class PanServiceImpl implements PanService {
    private static final String AUTH_CODE_ID = "admin";
    private static final int AUTH_CODE_TYPE = 2;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static PanServiceImpl instance;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private PanServiceImpl() {
    }

    public static synchronized PanServiceImpl getInstance() {
        if (instance == null) {
            instance = new PanServiceImpl();
        }
        return instance;
    }

    private interface ApiCallback {
        void onSuccess(Object data);

        void onError(int code, String message);
    }

    private String apiBase() {
        String base = Config.getPanServerAddress();
        if (TextUtils.isEmpty(base)) {
            return null;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/v1";
    }

    private String host() {
        return extractHost(Config.getPanServerAddress());
    }

    // ==================== 请求底层 ====================

    private void post(final String path, final Map<String, Object> params, final ApiCallback callback) {
        request(0, path, params, callback);
    }

    private void get(final String path, final ApiCallback callback) {
        request(1, path, null, callback);
    }

    private void request(final int method, final String path, final Map<String, Object> params, final ApiCallback callback) {
        final String base = apiBase();
        if (TextUtils.isEmpty(base)) {
            callback.onError(-1, "网盘服务未配置");
            return;
        }
        ChatManager.Instance().getAuthCode(AUTH_CODE_ID, AUTH_CODE_TYPE, host(), new GeneralCallback2() {
            @Override
            public void onSuccess(String authCode) {
                Request.Builder builder = new Request.Builder().url(base + path)
                    .addHeader("authCode", authCode)
                    .addHeader("Content-Type", "application/json");
                if (method == 1) {
                    builder.get();
                } else {
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
                    builder.post(RequestBody.create(JSON, jsonBody.toString()));
                }
                okHttpClient.newCall(builder.build()).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        postError(callback, -1, e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            String body = response.body() == null ? "" : response.body().string();
                            if (!response.isSuccessful()) {
                                postError(callback, response.code(), response.message());
                                return;
                            }
                            JSONObject jsonObject = new JSONObject(body);
                            int code = jsonObject.optInt("code", -1);
                            if (code != 0) {
                                postError(callback, code, jsonObject.optString("message", "未知错误"));
                                return;
                            }
                            Object data = jsonObject.opt("data");
                            if (data == JSONObject.NULL) {
                                data = null;
                            }
                            postSuccess(callback, data);
                        } catch (Exception e) {
                            postError(callback, -1, "解析失败: " + e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onFail(int errorCode) {
                postError(callback, errorCode, "获取认证码失败");
            }
        });
    }

    private void postSuccess(ApiCallback callback, Object data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private void postError(ApiCallback callback, int code, String message) {
        mainHandler.post(() -> callback.onError(code, message));
    }

    private static String extractHost(String url) {
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

    // ==================== 解析辅助 ====================

    private static JSONArray asArray(Object data) {
        return data instanceof JSONArray ? (JSONArray) data : new JSONArray();
    }

    private static JSONObject asObject(Object data) {
        return data instanceof JSONObject ? (JSONObject) data : null;
    }

    private static List<PanSpace> parseSpaces(Object data) {
        List<PanSpace> list = new ArrayList<>();
        JSONArray array = asArray(data);
        for (int i = 0; i < array.length(); i++) {
            PanSpace space = PanSpace.fromJson(array.optJSONObject(i));
            if (space != null) {
                list.add(space);
            }
        }
        return list;
    }

    private static List<PanFile> parseFiles(Object data) {
        List<PanFile> list = new ArrayList<>();
        JSONArray array = asArray(data);
        for (int i = 0; i < array.length(); i++) {
            PanFile file = PanFile.fromJson(array.optJSONObject(i));
            if (file != null) {
                list.add(file);
            }
        }
        return list;
    }

    private static Map<String, Object> params(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    // ==================== 空间 ====================

    @Override
    public void getSpaces(final ResultCallback<List<PanSpace>> callback) {
        post("/spaces/list", null, new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(parseSpaces(data));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void getVisibleSpaces(final ResultCallback<List<PanSpace>> callback) {
        getSpaces(new ResultCallback<List<PanSpace>>() {
            @Override
            public void onSuccess(List<PanSpace> spaces) {
                String me = ChatManager.Instance().getUserId();
                List<PanSpace> visible = new ArrayList<>();
                for (PanSpace space : spaces) {
                    if (space.isGlobalPublic() || TextUtils.equals(space.ownerId, me)) {
                        visible.add(space);
                    }
                }
                Collections.sort(visible, new Comparator<PanSpace>() {
                    private int order(PanSpace s) {
                        if (s.isUserPrivate()) return 0;
                        if (s.isUserPublic()) return 1;
                        return 2;
                    }

                    @Override
                    public int compare(PanSpace a, PanSpace b) {
                        return Integer.compare(order(a), order(b));
                    }
                });
                callback.onSuccess(visible);
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void getMySpaces(final ResultCallback<List<PanSpace>> callback) {
        post("/spaces/my", null, new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(parseSpaces(data));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void getUserPublicSpace(String targetUserId, final ResultCallback<PanSpace> callback) {
        post("/spaces/user/public", params("targetUserId", targetUserId), new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(PanSpace.fromJson(asObject(data)));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void checkSpaceWritePermission(long spaceId, final ResultCallback<Boolean> callback) {
        get("/permission/space/" + spaceId + "/write", new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(Boolean.TRUE.equals(data));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    // ==================== 文件 ====================

    @Override
    public void getSpaceFiles(long spaceId, long parentId, final ResultCallback<List<PanFile>> callback) {
        post("/spaces/files", params("spaceId", spaceId, "parentId", parentId), new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(parseFiles(data));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void createFolder(long spaceId, long parentId, String name, final ResultCallback<PanFile> callback) {
        post("/files/folder", params("spaceId", spaceId, "parentId", parentId, "name", name), new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(PanFile.fromJson(asObject(data)));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void createFile(long spaceId, long parentId, String name, long size, String mimeType,
                           String md5, String storageUrl, boolean copy, final ResultCallback<PanFile> callback) {
        Map<String, Object> map = params(
            "spaceId", spaceId,
            "parentId", parentId,
            "name", name,
            "size", size,
            "storageUrl", storageUrl,
            "copy", copy);
        if (!TextUtils.isEmpty(mimeType)) {
            map.put("mimeType", mimeType);
        }
        if (!TextUtils.isEmpty(md5)) {
            map.put("md5", md5);
        }
        post("/files", map, new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(PanFile.fromJson(asObject(data)));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void deleteFile(long fileId, final OperationCallback callback) {
        post("/files/delete", params("fileId", fileId), operationCallback(callback));
    }

    @Override
    public void renameFile(long fileId, String newName, final OperationCallback callback) {
        post("/files/rename", params("fileId", fileId, "newName", newName), operationCallback(callback));
    }

    @Override
    public void moveFile(long fileId, long targetSpaceId, long targetParentId, final OperationCallback callback) {
        post("/files/move", params("fileId", fileId, "targetSpaceId", targetSpaceId, "targetParentId", targetParentId),
            operationCallback(callback));
    }

    @Override
    public void copyFile(long fileId, long targetSpaceId, long targetParentId, final OperationCallback callback) {
        post("/files/copy", params("fileId", fileId, "targetSpaceId", targetSpaceId, "targetParentId", targetParentId),
            operationCallback(callback));
    }

    private ApiCallback operationCallback(final OperationCallback callback) {
        return new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(int code, String message) {
                if (callback != null) {
                    callback.onError(code, message);
                }
            }
        };
    }

    @Override
    public void getFileDownloadUrl(long fileId, int versionNo, final ResultCallback<PanFileUrl> callback) {
        Map<String, Object> map = params("fileId", fileId);
        if (versionNo > 0) {
            map.put("versionNo", versionNo);
        }
        post("/files/url", map, new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(PanFileUrl.fromJson(asObject(data)));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void getVersions(long fileId, final ResultCallback<List<PanVersion>> callback) {
        post("/versions/list", params("fileId", fileId), new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                List<PanVersion> list = new ArrayList<>();
                JSONArray array = asArray(data);
                for (int i = 0; i < array.length(); i++) {
                    PanVersion version = PanVersion.fromJson(array.optJSONObject(i));
                    if (version != null) {
                        list.add(version);
                    }
                }
                callback.onSuccess(list);
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void restoreVersion(long fileId, int versionNo, final OperationCallback callback) {
        post("/versions/restore", params("fileId", fileId, "versionNo", versionNo), operationCallback(callback));
    }

    // ==================== 分享 ====================

    @Override
    public void getShares(long fileId, final ResultCallback<List<PanShare>> callback) {
        post("/shares/list", params("fileId", fileId), new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                List<PanShare> list = new ArrayList<>();
                JSONArray array = asArray(data);
                for (int i = 0; i < array.length(); i++) {
                    PanShare share = PanShare.fromJson(array.optJSONObject(i));
                    if (share != null) {
                        list.add(share);
                    }
                }
                callback.onSuccess(list);
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void setShare(long fileId, boolean isGroup, String targetId, boolean canEdit, final OperationCallback callback) {
        post("/shares/add", params(
            "fileId", fileId,
            "targetType", isGroup ? "GROUP" : "USER",
            "targetId", targetId,
            "permission", canEdit ? "EDIT" : "VIEW"), operationCallback(callback));
    }

    @Override
    public void removeShare(long shareId, final OperationCallback callback) {
        post("/shares/remove", params("shareId", shareId), operationCallback(callback));
    }

    // ==================== 在线文档 ====================

    @Override
    public void getRecentDocs(final ResultCallback<List<PanDocEntry>> callback) {
        post("/docs/recent", null, new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(parseDocEntries(data, false));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void getSharedWithMe(final ResultCallback<List<PanDocEntry>> callback) {
        post("/shares/with-me", null, new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(parseDocEntries(data, true));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    private static List<PanDocEntry> parseDocEntries(Object data, boolean sharedWithMe) {
        List<PanDocEntry> list = new ArrayList<>();
        JSONArray array = asArray(data);
        for (int i = 0; i < array.length(); i++) {
            PanDocEntry entry = PanDocEntry.fromJson(array.optJSONObject(i), sharedWithMe);
            if (entry != null && entry.file != null) {
                list.add(entry);
            }
        }
        return list;
    }

    @Override
    public void removeRecentDoc(long fileId, final OperationCallback callback) {
        post("/docs/recent/remove", params("fileId", fileId), operationCallback(callback));
    }

    @Override
    public void createDoc(String type, String name, final ResultCallback<PanFile> callback) {
        Map<String, Object> map = params("type", type);
        if (!TextUtils.isEmpty(name)) {
            map.put("name", name);
        }
        post("/docs/create", map, new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(PanFile.fromJson(asObject(data)));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    @Override
    public void getDocOptions(final ResultCallback<Boolean> callback) {
        post("/docs/options", null, new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                JSONObject object = asObject(data);
                callback.onSuccess(object != null && object.optBoolean("mobileEdit", false));
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    // ==================== 上传 ====================

    @Override
    public void uploadFile(String localPath, final long spaceId, final long parentId, String remoteName,
                           final PanUploadCancelToken cancelToken, final ProgressCallback progressCallback,
                           final ResultCallback<PanFile> callback) {
        final File file = new File(localPath);
        if (!file.exists()) {
            callback.onError(-1, "文件不存在");
            return;
        }
        final String name = TextUtils.isEmpty(remoteName) ? file.getName() : remoteName;
        ChatManager.Instance().uploadMediaFile(localPath, MessageContentMediaType.FILE.getValue(), new UploadMediaCallback() {
            @Override
            public void onSuccess(String storageUrl) {
                if (cancelToken != null && cancelToken.isCancelled()) {
                    callback.onError(-2, "上传已取消");
                    return;
                }
                createFile(spaceId, parentId, name, file.length(), guessMimeType(name), null, storageUrl, false, callback);
            }

            @Override
            public void onProgress(long uploaded, long total) {
                if (progressCallback != null && total > 0) {
                    progressCallback.onProgress(uploaded * 1.0 / total);
                }
            }

            @Override
            public void onFail(int errorCode) {
                if (cancelToken != null && cancelToken.isCancelled()) {
                    callback.onError(-2, "上传已取消");
                } else {
                    callback.onError(errorCode, "上传失败");
                }
            }
        });
    }

    private static String guessMimeType(String name) {
        int idx = name.lastIndexOf('.');
        if (idx > 0 && idx < name.length() - 1) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(idx + 1).toLowerCase());
            if (!TextUtils.isEmpty(mime)) {
                return mime;
            }
        }
        return "application/octet-stream";
    }
}
