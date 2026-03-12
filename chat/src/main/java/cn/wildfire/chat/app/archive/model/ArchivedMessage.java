/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.archive.model;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 归档消息模型
 * <p>
 * 对应备份服务 API 返回的消息结构
 * </p>
 */
public class ArchivedMessage {

    /// 消息唯一ID
    public long mid;

    /// 发送者用户ID
    public String senderId;

    /// 会话类型：0单聊, 1群组, 2聊天室, 3频道
    public int convType;

    /// 会话目标（用户ID或群组ID）
    public String convTarget;

    /// 会话线路
    public int convLine;

    /// 消息内容类型
    public int contentType;

    /// 消息内容（已解析的 payload）
    public ArchiveMessagePayload payload;

    /// 可搜索文本
    public String searchableKey;

    /// 当前用户ID（用户视角）
    public String userId;

    /// 消息发送时间（ISO 8601格式，UTC时间）
    public String messageDt;

    /// 用户哈希值（用于分区）
    public int userHash;

    /**
     * 从JSON对象创建归档消息对象
     *
     * @param json JSON对象
     * @return ArchivedMessage对象，解析失败返回null
     */
    public static ArchivedMessage fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }

        ArchivedMessage message = new ArchivedMessage();
        message.mid = json.optLong("mid", 0);
        message.senderId = json.optString("senderId", "");
        message.convType = json.optInt("convType", 0);
        message.convTarget = json.optString("convTarget", "");
        message.convLine = json.optInt("convLine", 0);
        message.contentType = json.optInt("contentType", 0);

        // 解析 payload
        JSONObject payloadJson = json.optJSONObject("payload");
        if (payloadJson != null) {
            message.payload = ArchiveMessagePayload.fromJson(payloadJson);
        }

        message.searchableKey = json.optString("searchableKey", null);
        message.userId = json.optString("userId", "");
        message.messageDt = json.optString("messageDt", "");
        message.userHash = json.optInt("userHash", 0);

        return message;
    }

    /**
     * 获取本地时间格式的消息时间
     *
     * @return 本地时间的Date对象
     */
    public Date getLocalMessageDate() {
        if (messageDt == null || messageDt.isEmpty()) {
            return new Date();
        }

        try {
            // ISO 8601 格式解析
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(messageDt);
            if (date != null) {
                return date;
            }
        } catch (Exception e) {
            // 尝试不带毫秒的格式
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(messageDt);
                if (date != null) {
                    return date;
                }
            } catch (Exception e2) {
                // 忽略解析错误
            }
        }

        return new Date();
    }

    @Override
    public String toString() {
        return "ArchivedMessage{" +
                "mid=" + mid +
                ", senderId='" + senderId + '\'' +
                ", convType=" + convType +
                ", convTarget='" + convTarget + '\'' +
                ", contentType=" + contentType +
                '}';
    }
}
