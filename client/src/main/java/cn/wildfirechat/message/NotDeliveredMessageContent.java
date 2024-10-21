/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_NOT_DELIVERED;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.Conversation;

@ContentTag(type = ContentType_NOT_DELIVERED, flag = PersistFlag.Persist)
public class NotDeliveredMessageContent extends NotificationMessageContent {
    // 请求的类型，1 发送消息，2 撤回消息，3 删除消息
    private int type;

    // 发送的消息 uid
    private long messageUid;

    // 是全部失败，还是部分失败
    private boolean allFailure;

    // 部分失败时，失败的用户 id 列表
    private List<String> userIds;

    // 归属IM服务请求桥接服务出现的错误，有可能是桥接服务没有配置，或者不可用。
    private int localImErrorCode;

    // 归属桥接服务出现的错误
    private int localBridgeErrorCode;

    // 远端桥接服务出现的错误
    private int remoteBridgeErrorCode;

    // 远端IM服务出现的错误
    private int remoteServerErrorCode;

    // 错误提示信息
    private String errorMessage;


    @Override
    public String formatNotification(Message message) {
        String desc;
        if (this.allFailure) {
            desc = "消息未能送达";
        } else {
            if (message.conversation.type == Conversation.ConversationType.Single) {
                desc = "消息未能送达";
            } else {
                desc = "消息未能送达部分用户";
            }
        }
        return desc;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        JSONObject obj = new JSONObject();
        try {
            obj.put("mid", this.messageUid);
            obj.put("all", this.allFailure);
            obj.put("us", this.userIds);
            obj.put("lme", this.localImErrorCode);
            obj.put("lbe", this.localBridgeErrorCode);
            obj.put("rbe", this.remoteBridgeErrorCode);
            obj.put("rme", this.remoteServerErrorCode);
            obj.put("em", this.errorMessage);
            payload.binaryContent = obj.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            if (payload.binaryContent != null) {
                JSONObject obj = new JSONObject(new String(payload.binaryContent));
                this.messageUid = obj.optLong("mid");
                this.allFailure = obj.optBoolean("all");
                this.userIds = new ArrayList<>();
                JSONArray arr = obj.optJSONArray("us");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        this.userIds.add(arr.getString(i));
                    }
                }
                this.localImErrorCode = obj.optInt("lme");
                this.localBridgeErrorCode = obj.optInt("lbe");
                this.remoteBridgeErrorCode = obj.optInt("rbe");
                this.remoteServerErrorCode = obj.optInt("rme");
                this.errorMessage = obj.optString("em");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.type);
        dest.writeLong(this.messageUid);
        dest.writeByte(this.allFailure ? (byte) 1 : (byte) 0);
        dest.writeStringList(this.userIds);
        dest.writeInt(this.localImErrorCode);
        dest.writeInt(this.localBridgeErrorCode);
        dest.writeInt(this.remoteBridgeErrorCode);
        dest.writeInt(this.remoteServerErrorCode);
        dest.writeString(this.errorMessage);
    }

    public void readFromParcel(Parcel source) {
        this.type = source.readInt();
        this.messageUid = source.readLong();
        this.allFailure = source.readByte() != 0;
        this.userIds = source.createStringArrayList();
        this.localImErrorCode = source.readInt();
        this.localBridgeErrorCode = source.readInt();
        this.remoteBridgeErrorCode = source.readInt();
        this.remoteServerErrorCode = source.readInt();
        this.errorMessage = source.readString();
    }

    public NotDeliveredMessageContent() {
    }

    protected NotDeliveredMessageContent(Parcel in) {
        super(in);
        this.type = in.readInt();
        this.messageUid = in.readLong();
        this.allFailure = in.readByte() != 0;
        this.userIds = in.createStringArrayList();
        this.localImErrorCode = in.readInt();
        this.localBridgeErrorCode = in.readInt();
        this.remoteBridgeErrorCode = in.readInt();
        this.remoteServerErrorCode = in.readInt();
        this.errorMessage = in.readString();
    }

    public static final Creator<NotDeliveredMessageContent> CREATOR = new Creator<NotDeliveredMessageContent>() {
        @Override
        public NotDeliveredMessageContent createFromParcel(Parcel source) {
            return new NotDeliveredMessageContent(source);
        }

        @Override
        public NotDeliveredMessageContent[] newArray(int size) {
            return new NotDeliveredMessageContent[size];
        }
    };
}
