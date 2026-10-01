/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * 网盘空间：全局公共 / 我的公共 / 我的私有。对齐服务端 SpaceVO。
 */
public class PanSpace {
    public static final String TYPE_GLOBAL_PUBLIC = "GLOBAL_PUBLIC";
    public static final String TYPE_USER_PUBLIC = "USER_PUBLIC";
    public static final String TYPE_USER_PRIVATE = "USER_PRIVATE";

    public long spaceId;
    public String spaceType;
    public String ownerId;
    public String name;
    public long totalQuota;
    public long usedQuota;
    public int fileCount;
    public int folderCount;
    public boolean autoInit;
    public String createdAt;
    public String updatedAt;
    public boolean canManage;

    public static PanSpace fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        PanSpace space = new PanSpace();
        space.spaceId = json.optLong("spaceId", json.optLong("id", 0));
        space.spaceType = normalizeType(json.optString("spaceType", json.optString("type", TYPE_GLOBAL_PUBLIC)));
        space.ownerId = json.optString("ownerId", "");
        space.name = json.optString("name", "");
        space.totalQuota = json.optLong("totalQuota", 0);
        space.usedQuota = json.optLong("usedQuota", 0);
        space.fileCount = json.optInt("fileCount", 0);
        space.folderCount = json.optInt("folderCount", 0);
        space.autoInit = json.optBoolean("autoInit", false);
        space.createdAt = json.optString("createdAt", "");
        space.updatedAt = json.optString("updatedAt", "");
        space.canManage = json.optBoolean("canManage", false);
        return space;
    }

    /**
     * 兼容服务端返回字符串（GLOBAL_PUBLIC）或数字（0/1/2）。
     */
    private static String normalizeType(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return TYPE_GLOBAL_PUBLIC;
        }
        switch (raw.toUpperCase()) {
            case "0":
            case TYPE_GLOBAL_PUBLIC:
                return TYPE_GLOBAL_PUBLIC;
            case "1":
            case TYPE_USER_PUBLIC:
                return TYPE_USER_PUBLIC;
            case "2":
            case TYPE_USER_PRIVATE:
                return TYPE_USER_PRIVATE;
            default:
                return raw.toUpperCase();
        }
    }

    public boolean isGlobalPublic() {
        return TYPE_GLOBAL_PUBLIC.equals(spaceType);
    }

    public boolean isUserPublic() {
        return TYPE_USER_PUBLIC.equals(spaceType);
    }

    public boolean isUserPrivate() {
        return TYPE_USER_PRIVATE.equals(spaceType);
    }

    /**
     * 能不能往里写：别人的公共空间只读，其余以服务端校验为准。
     */
    public boolean canWrite(String currentUserId) {
        if (isUserPublic()) {
            return !TextUtils.isEmpty(currentUserId) && TextUtils.equals(ownerId, currentUserId);
        }
        return true;
    }
}
