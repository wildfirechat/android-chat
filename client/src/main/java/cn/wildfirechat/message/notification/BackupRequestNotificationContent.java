/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import android.os.Parcel;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 备份请求通知消息
 * <p>
 * 当Android端请求备份到PC端时发送此通知消息。
 * 包含会话列表、是否包含媒体文件等信息。
 * 此消息不会被持久化，仅用于实时传输。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = MessageContentType.ContentType_Backup_Request, flag = PersistFlag.Transparent)
public class BackupRequestNotificationContent extends NotificationMessageContent {
    private static final String TAG = "BackupRequest";

    /**
     * 会话列表（JSON字符串）
     * 格式: [{conversation: {type, target, line}, messageCount: int}, ...]
     */
    private String conversationsJson;

    /**
     * 是否包含媒体文件
     */
    private boolean includeMedia;

    /**
     * 请求时间戳
     */
    private long timestamp;

    public String getConversationsJson() {
        return conversationsJson;
    }

    public void setConversationsJson(String conversationsJson) {
        this.conversationsJson = conversationsJson;
    }

    public boolean isIncludeMedia() {
        return includeMedia;
    }

    public void setIncludeMedia(boolean includeMedia) {
        this.includeMedia = includeMedia;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public BackupRequestNotificationContent() {
    }

    public BackupRequestNotificationContent(String conversationsJson, boolean includeMedia, long timestamp) {
        this.conversationsJson = conversationsJson;
        this.includeMedia = includeMedia;
        this.timestamp = timestamp;
    }

    @Override
    public String formatNotification(Message message) {
        return "Request backup to PC";
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        JSONObject obj = new JSONObject();
        try {
            if (conversationsJson != null) {
                obj.put("c", conversationsJson);
            }
            obj.put("m", includeMedia);
            obj.put("t", timestamp);
            payload.binaryContent = obj.toString().getBytes();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to encode BackupRequestNotificationContent", e);
        }
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            String jsonStr = new String(payload.binaryContent);
            JSONObject obj = new JSONObject(jsonStr);
            conversationsJson = obj.optString("c");
            includeMedia = obj.optBoolean("m");
            timestamp = obj.optLong("t");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to decode BackupRequestNotificationContent", e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.conversationsJson);
        dest.writeInt(this.includeMedia ? 1 : 0);
        dest.writeLong(this.timestamp);
    }

    protected BackupRequestNotificationContent(Parcel in) {
        super(in);
        this.conversationsJson = in.readString();
        this.includeMedia = in.readInt() == 1;
        this.timestamp = in.readLong();
    }

    public static final Creator<BackupRequestNotificationContent> CREATOR = new Creator<BackupRequestNotificationContent>() {
        @Override
        public BackupRequestNotificationContent createFromParcel(Parcel source) {
            return new BackupRequestNotificationContent(source);
        }

        @Override
        public BackupRequestNotificationContent[] newArray(int size) {
            return new BackupRequestNotificationContent[size];
        }
    };
}
