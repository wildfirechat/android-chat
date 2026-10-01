/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档首页的一行：最近打开 / 共享给我的一个文件。
 */
public class PanDocEntry {
    public PanFile file;
    public boolean canEdit;
    public String time;
    public List<PanShareSource> sources = new ArrayList<>();

    public static PanDocEntry fromJson(JSONObject json, boolean sharedWithMe) {
        if (json == null) {
            return null;
        }
        PanDocEntry entry = new PanDocEntry();
        entry.file = PanFile.fromJson(json.optJSONObject("file"));
        entry.canEdit = "EDIT".equalsIgnoreCase(json.optString("permission", "VIEW"));
        entry.time = json.optString(sharedWithMe ? "sharedAt" : "openedAt", "");
        return entry;
    }
}
