/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import org.json.JSONObject;

/**
 * 「共享给我」里的一条来源：某人直接分享 / 经某个群分享。
 */
public class PanShareSource {
    public boolean group;
    public String groupName;
    public String sharerName;

    public static PanShareSource fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        PanShareSource source = new PanShareSource();
        source.group = "GROUP".equalsIgnoreCase(json.optString("targetType", ""));
        source.groupName = json.optString("targetName", "");
        source.sharerName = json.optString("createdByName", "");
        return source;
    }
}
