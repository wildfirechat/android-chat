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
 * 备份响应通知消息
 * <p>
 * PC端响应Android端的备份请求。
 * 包含是否同意备份及服务器连接信息（IP和端口）。
 * 此消息不会被持久化，仅用于实时传输。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = MessageContentType.ContentType_Backup_Response, flag = PersistFlag.Transparent)
public class BackupResponseNotificationContent extends NotificationMessageContent {
    private static final String TAG = "BackupResponse";

    /**
     * 是否同意备份请求
     */
    private boolean approved;

    /**
     * 服务器IP地址（同意时有效）
     */
    private String serverIP;

    /**
     * 服务器端口（同意时有效）
     */
    private int serverPort;

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public BackupResponseNotificationContent() {
    }

    /**
     * 创建拒绝消息
     */
    public static BackupResponseNotificationContent createRejectedResponse() {
        BackupResponseNotificationContent content = new BackupResponseNotificationContent();
        content.approved = false;
        return content;
    }

    /**
     * 创建同意消息
     */
    public static BackupResponseNotificationContent createApprovedResponse(String ip, int port) {
        BackupResponseNotificationContent content = new BackupResponseNotificationContent();
        content.approved = true;
        content.serverIP = ip;
        content.serverPort = port;
        return content;
    }

    @Override
    public String formatNotification(Message message) {
        return approved ? "Approved backup request" : "Rejected backup request";
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        JSONObject obj = new JSONObject();
        try {
            obj.put("a", approved);
            if (approved) {
                if (serverIP != null) {
                    obj.put("ip", serverIP);
                }
                obj.put("p", serverPort);
            }
            payload.binaryContent = obj.toString().getBytes();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to encode BackupResponseNotificationContent", e);
        }
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            String jsonStr = new String(payload.binaryContent);
            JSONObject obj = new JSONObject(jsonStr);
            approved = obj.optBoolean("a");
            if (approved) {
                serverIP = obj.optString("ip");
                serverPort = obj.optInt("p");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to decode BackupResponseNotificationContent", e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.approved ? 1 : 0);
        dest.writeString(this.serverIP);
        dest.writeInt(this.serverPort);
    }

    protected BackupResponseNotificationContent(Parcel in) {
        super(in);
        this.approved = in.readInt() == 1;
        this.serverIP = in.readString();
        this.serverPort = in.readInt();
    }

    public static final Creator<BackupResponseNotificationContent> CREATOR = new Creator<BackupResponseNotificationContent>() {
        @Override
        public BackupResponseNotificationContent createFromParcel(Parcel source) {
            return new BackupResponseNotificationContent(source);
        }

        @Override
        public BackupResponseNotificationContent[] newArray(int size) {
            return new BackupResponseNotificationContent[size];
        }
    };
}
