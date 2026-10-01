/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.net.Uri;

import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.pan.model.PanDocEntry;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanFileUrl;
import cn.wildfire.chat.kit.pan.model.PanShare;
import cn.wildfire.chat.kit.pan.model.PanSpace;
import cn.wildfire.chat.kit.pan.model.PanUploadCancelToken;
import cn.wildfire.chat.kit.pan.model.PanVersion;

/**
 * 网盘 / 在线文档服务接口。
 * <p>
 * 实现放在 {@code :chat}（{@code PanServiceImpl}，走独立 wf-pan-server：
 * {@code PAN_SERVER + /api/v1}，每个请求带 {@code authCode} 头，响应体 {@code {code,message,data}}），
 * UI 只依赖本接口与 {@link PanServiceProvider}。
 */
public interface PanService {

    interface ResultCallback<T> {
        void onSuccess(T data);

        void onError(int code, String message);
    }

    interface OperationCallback {
        void onSuccess();

        void onError(int code, String message);
    }

    interface ProgressCallback {
        void onProgress(double progress);
    }

    // ---- 空间 ----

    void getSpaces(ResultCallback<List<PanSpace>> callback);

    void getVisibleSpaces(ResultCallback<List<PanSpace>> callback);

    void getMySpaces(ResultCallback<List<PanSpace>> callback);

    void getUserPublicSpace(String targetUserId, ResultCallback<PanSpace> callback);

    void checkSpaceWritePermission(long spaceId, ResultCallback<Boolean> callback);

    // ---- 文件 ----

    void getSpaceFiles(long spaceId, long parentId, ResultCallback<List<PanFile>> callback);

    void createFolder(long spaceId, long parentId, String name, ResultCallback<PanFile> callback);

    void createFile(long spaceId, long parentId, String name, long size, String mimeType,
                    String md5, String storageUrl, boolean copy, ResultCallback<PanFile> callback);

    void deleteFile(long fileId, OperationCallback callback);

    void renameFile(long fileId, String newName, OperationCallback callback);

    void moveFile(long fileId, long targetSpaceId, long targetParentId, OperationCallback callback);

    void copyFile(long fileId, long targetSpaceId, long targetParentId, OperationCallback callback);

    void getFileDownloadUrl(long fileId, int versionNo, ResultCallback<PanFileUrl> callback);

    void getVersions(long fileId, ResultCallback<List<PanVersion>> callback);

    void restoreVersion(long fileId, int versionNo, OperationCallback callback);

    // ---- 分享 ----

    void getShares(long fileId, ResultCallback<List<PanShare>> callback);

    void setShare(long fileId, boolean isGroup, String targetId, boolean canEdit, OperationCallback callback);

    void removeShare(long shareId, OperationCallback callback);

    // ---- 在线文档 ----

    void getRecentDocs(ResultCallback<List<PanDocEntry>> callback);

    void getSharedWithMe(ResultCallback<List<PanDocEntry>> callback);

    void removeRecentDoc(long fileId, OperationCallback callback);

    void createDoc(String type, String name, ResultCallback<PanFile> callback);

    void getDocOptions(ResultCallback<Boolean> callback);

    // ---- 上传 ----

    void uploadFile(String localPath, long spaceId, long parentId, String remoteName,
                    PanUploadCancelToken cancelToken, ProgressCallback progressCallback,
                    ResultCallback<PanFile> callback);

    // ==================== 静态工具 ====================

    /**
     * 在线文档 H5 页面根地址，带尾斜杠。来自独立的 wf-pan-server：{@code PAN_SERVER + /doc/}。
     */
    static String docBase() {
        String base = Config.getPanServerAddress();
        if (base == null) {
            return null;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/doc/";
    }

    /**
     * 打开网盘内的文件：能不能编辑由服务端按权限决定。
     */
    static String docOpenUrl(long fileId) {
        String base = docBase();
        return base == null ? null : base + "open?fileId=" + fileId;
    }

    /**
     * 按链接只读打开一个在线文档：文件不在网盘里（聊天文件消息等），服务端代理取内容。
     */
    static String docViewUrl(String url, String name) {
        String base = docBase();
        if (base == null || url == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(base).append("open?url=").append(Uri.encode(url));
        if (name != null && !name.isEmpty()) {
            sb.append("&name=").append(Uri.encode(name));
        }
        return sb.toString();
    }

    /**
     * 开源许可页：编辑引擎是 AGPL，文档首页要留这个入口。
     */
    static String docLicensesUrl() {
        String base = docBase();
        return base == null ? null : base + "licenses.html";
    }

    /**
     * 是不是在线文档页面地址。主备地址都认：卡片是对方按他当时所在网络的地址发的。
     */
    static boolean isDocUrl(String url) {
        if (url == null) {
            return false;
        }
        String main = Config.PAN_SERVER_ADDRESS;
        String backup = Config.PAN_SERVER_BACKUP_ADDRESS;
        for (String base : new String[]{main, backup}) {
            if (base == null || base.isEmpty()) {
                continue;
            }
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            if (url.startsWith(base + "/doc/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在线文档能打开的格式，与服务端四组（word / cell / slide / pdf）一致。
     */
    String[] ONLINE_DOC_EXTENSIONS = {
        "doc", "docx", "docm", "dot", "dotx", "dotm", "odt", "ott", "rtf", "txt",
        "wps", "wpt", "fodt", "mht", "mhtml", "htm", "html", "epub", "fb2",
        "xls", "xlsx", "xlsm", "xlt", "xltx", "xltm", "xlsb", "ods", "ots", "csv",
        "et", "ett", "fods",
        "ppt", "pptx", "pptm", "pot", "potx", "potm", "pps", "ppsx", "ppsm",
        "odp", "otp", "dps", "dpt", "fodp",
        "pdf", "djvu", "xps", "oxps",
    };

    static boolean isOnlineDocName(String name) {
        if (name == null) {
            return false;
        }
        int idx = name.lastIndexOf('.');
        if (idx <= 0 || idx == name.length() - 1) {
            return false;
        }
        String ext = name.substring(idx + 1).toLowerCase();
        for (String docExt : ONLINE_DOC_EXTENSIONS) {
            if (docExt.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 网盘展示风格的大小：带一位小数，如 {@code 1.5 GB}。
     */
    static String formatPanSize(long bytes) {
        if (bytes >= 1073741824L) {
            return String.format(java.util.Locale.US, "%.1f GB", bytes / 1073741824.0);
        }
        if (bytes >= 1048576L) {
            return String.format(java.util.Locale.US, "%.1f MB", bytes / 1048576.0);
        }
        if (bytes >= 1024L) {
            return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }
}
