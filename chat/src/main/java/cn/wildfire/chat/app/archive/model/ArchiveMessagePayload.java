/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.archive.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.core.MessagePayload;

/**
 * 消息内容 Payload
 * <p>
 * 对应服务端 API 返回的 payload 结构（已解析的消息内容）
 * </p>
 */
public class ArchiveMessagePayload {

    /// 消息类型
    public int type;

    /// 消息文本内容（如文本消息的实际内容、位置标题、链接标题等）
    public String content;

    /// 可搜索内容
    public String searchableContent;

    /// 推送内容
    public String pushContent;

    /// 推送数据
    public String pushData;

    /// @类型：0-不提醒，1-所有人，2-部分人
    public int mentionedType;

    /// @的目标用户ID列表
    public List<String> mentionedTargets;

    /// 媒体文件远程URL（图片、语音、文件、视频等）
    public String remoteMediaUrl;

    /// 扩展字段（JSON格式，包含类型特定的额外信息）
    public String extra;

    /// 原始Base64数据（解析失败时保留）
    public String binaryContent;

    /// 子类型（如Typing消息的类型）
    public int subType;

    /**
     * 从JSON对象创建 payload 对象
     *
     * @param json JSON对象
     * @return ArchiveMessagePayload对象，解析失败返回null
     */
    public static ArchiveMessagePayload fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }

        ArchiveMessagePayload payload = new ArchiveMessagePayload();
        payload.type = json.optInt("type", 0);
        payload.content = json.optString("content", null);
        payload.searchableContent = json.optString("searchableContent", null);
        payload.pushContent = json.optString("pushContent", null);
        payload.pushData = json.optString("pushData", null);
        payload.mentionedType = json.optInt("mentionedType", 0);

        // 解析 mentionedTargets 数组
        JSONArray mentionedTargetsArray = json.optJSONArray("mentionedTargets");
        if (mentionedTargetsArray != null) {
            payload.mentionedTargets = new ArrayList<>();
            for (int i = 0; i < mentionedTargetsArray.length(); i++) {
                String target = mentionedTargetsArray.optString(i, null);
                if (target != null) {
                    payload.mentionedTargets.add(target);
                }
            }
        }

        payload.remoteMediaUrl = json.optString("remoteMediaUrl", null);
        payload.extra = json.optString("extra", null);
        payload.binaryContent = json.optString("binaryContent", null);
        payload.subType = json.optInt("subType", 0);

        return payload;
    }

    /**
     * 将 payload 转换为 SDK 的 MessagePayload
     *
     * @param contentType 消息内容类型
     * @return SDK使用的MessagePayload对象
     */
    public MessagePayload toSDKPayload(int contentType) {
        MessagePayload sdkPayload = new MessagePayload();

        sdkPayload.type = contentType;
        sdkPayload.searchableContent = searchableContent;
        sdkPayload.pushContent = pushContent;
        sdkPayload.pushData = pushData;
        sdkPayload.content = content;
        sdkPayload.mentionedType = mentionedType;
        sdkPayload.mentionedTargets = mentionedTargets;
        sdkPayload.remoteMediaUrl = remoteMediaUrl;
        sdkPayload.extra = extra;
        sdkPayload.localMediaPath = "";

        // 如果有 binaryContent（Base64），转换为 byte[]
        if (binaryContent != null && !binaryContent.isEmpty()) {
            try {
                sdkPayload.binaryContent = android.util.Base64.decode(binaryContent, android.util.Base64.DEFAULT);
            } catch (Exception e) {
                // 解码失败，忽略
            }
        }

        return sdkPayload;
    }

    @Override
    public String toString() {
        return "ArchiveMessagePayload{" +
                "type=" + type +
                ", content='" + content + '\'' +
                '}';
    }
}
