/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import org.json.JSONObject;

/**
 * 文件的历史版本。对齐服务端 FileVersionVO。
 */
public class PanVersion {
    public long fileId;
    public int versionNo;
    public long size;
    public String source;
    public String editorId;
    public String editorName;
    public boolean current;
    public String createdAt;

    public static PanVersion fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        PanVersion version = new PanVersion();
        version.fileId = json.optLong("fileId", 0);
        version.versionNo = json.optInt("versionNo", 0);
        version.size = json.optLong("size", 0);
        version.source = json.optString("source", "");
        version.editorId = json.optString("editorId", "");
        version.editorName = json.optString("editorName", "");
        version.current = json.optBoolean("current", false);
        version.createdAt = json.optString("createdAt", "");
        return version;
    }
}
