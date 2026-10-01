/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import org.json.JSONObject;

/**
 * 一条分享记录。对齐服务端 ShareVO。
 */
public class PanShare {
    public long id;
    public long fileId;
    public String targetType;
    public String targetId;
    public String targetName;
    public String targetPortrait;
    public String permission;
    public String createdBy;
    public String createdByName;
    public String createdAt;

    public static PanShare fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        PanShare share = new PanShare();
        share.id = json.optLong("id", 0);
        share.fileId = json.optLong("fileId", 0);
        share.targetType = json.optString("targetType", "USER");
        share.targetId = json.optString("targetId", "");
        share.targetName = json.optString("targetName", "");
        share.targetPortrait = json.optString("targetPortrait", "");
        share.permission = json.optString("permission", "VIEW");
        share.createdBy = json.optString("createdBy", "");
        share.createdByName = json.optString("createdByName", "");
        share.createdAt = json.optString("createdAt", "");
        return share;
    }

    public boolean isGroup() {
        return "GROUP".equalsIgnoreCase(targetType);
    }

    public boolean canEdit() {
        return "EDIT".equalsIgnoreCase(permission);
    }
}
