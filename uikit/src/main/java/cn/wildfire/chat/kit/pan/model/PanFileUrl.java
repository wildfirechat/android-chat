/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import org.json.JSONObject;

/**
 * 取文件下载地址的返回。对齐服务端 FileUrlResponse。
 */
public class PanFileUrl {
    public long fileId;
    public String name;
    public String storageUrl;
    public int versionNo;
    public String permission;

    public static PanFileUrl fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        PanFileUrl url = new PanFileUrl();
        url.fileId = json.optLong("fileId", 0);
        url.name = json.optString("name", "");
        url.storageUrl = json.optString("storageUrl", "");
        url.versionNo = json.optInt("versionNo", 0);
        url.permission = json.optString("permission", "");
        return url;
    }

    public boolean canEdit() {
        return "EDIT".equalsIgnoreCase(permission);
    }
}
