/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import org.json.JSONObject;

/**
 * 网盘文件 / 文件夹。对齐服务端 FileVO。
 */
public class PanFile {
    public long fileId;
    public long spaceId;
    public long parentId;
    public String name;
    public boolean folder;
    public long size;
    public String mimeType;
    public String md5;
    public String storageUrl;
    public int childCount;
    public int versionNo;
    public String creatorId;
    public String creatorName;
    public String creatorPortrait;
    public String createdAt;
    public String updatedAt;

    public static PanFile fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        PanFile file = new PanFile();
        file.fileId = json.optLong("fileId", json.optLong("id", 0));
        file.spaceId = json.optLong("spaceId", 0);
        file.parentId = json.optLong("parentId", 0);
        file.name = json.optString("name", "");
        Object type = json.opt("type");
        file.folder = "FOLDER".equalsIgnoreCase(String.valueOf(type)) || "1".equals(String.valueOf(type));
        file.size = json.optLong("size", 0);
        file.mimeType = json.optString("mimeType", null);
        file.md5 = json.optString("md5", null);
        file.storageUrl = json.optString("storageUrl", null);
        file.childCount = json.optInt("childCount", 0);
        file.versionNo = json.optInt("versionNo", 0);
        file.creatorId = json.optString("creatorId", "");
        file.creatorName = json.optString("creatorName", null);
        file.creatorPortrait = json.optString("creatorPortrait", null);
        file.createdAt = json.optString("createdAt", "");
        file.updatedAt = json.optString("updatedAt", "");
        return file;
    }

    public boolean isFolder() {
        return folder;
    }

    public String extension() {
        if (folder || name == null) {
            return "";
        }
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(idx + 1).toLowerCase() : "";
    }
}
