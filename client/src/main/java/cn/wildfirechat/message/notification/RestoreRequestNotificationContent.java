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
 * 恢复请求通知消息
 * <p>
 * Android端请求PC端提供恢复备份列表的通知消息。
 * 用于跨设备数据恢复场景。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = MessageContentType.ContentType_Restore_Request, flag = PersistFlag.No_Persist)
public class RestoreRequestNotificationContent extends NotificationMessageContent {
    private static final String TAG = "RestoreRequest";

    /**
     * 请求时间戳
     */
    private long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public RestoreRequestNotificationContent() {
    }

    public RestoreRequestNotificationContent(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String formatNotification(Message message) {
        return "Request restore from PC";
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        JSONObject obj = new JSONObject();
        try {
            obj.put("t", timestamp);
            payload.binaryContent = obj.toString().getBytes();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to encode RestoreRequestNotificationContent", e);
        }
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            String jsonStr = new String(payload.binaryContent);
            JSONObject obj = new JSONObject(jsonStr);
            timestamp = obj.optLong("t");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to decode RestoreRequestNotificationContent", e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(this.timestamp);
    }

    protected RestoreRequestNotificationContent(Parcel in) {
        super(in);
        this.timestamp = in.readLong();
    }

    public static final Creator<RestoreRequestNotificationContent> CREATOR = new Creator<RestoreRequestNotificationContent>() {
        @Override
        public RestoreRequestNotificationContent createFromParcel(Parcel source) {
            return new RestoreRequestNotificationContent(source);
        }

        @Override
        public RestoreRequestNotificationContent[] newArray(int size) {
            return new RestoreRequestNotificationContent[size];
        }
    };
}
