/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 直播连麦信令消息
 * <p>
 * 通过单聊会话在主播与观众之间传递连麦请求/邀请/接受/拒绝信号。
 * 使用 Transparent flag，不持久化、不计数、不产生通知。
 * </p>
 */
@ContentTag(type = LiveCoStreamContent.TYPE, flag = PersistFlag.Transparent)
public class LiveCoStreamContent extends MessageContent {
    // 自定义消息type 1000以上
    public static final int TYPE = 1001;

    /** 观众发给主播：请求连麦 */
    public static final int ACTION_REQUEST = 0;
    /** 主播发给观众：邀请连麦 */
    public static final int ACTION_INVITE = 1;
    /** 接受连麦（主播接受请求 / 观众接受邀请），收到方负责调用 joinConference */
    public static final int ACTION_ACCEPT = 2;
    /** 拒绝连麦 */
    public static final int ACTION_REJECT = 3;

    private String callId;
    // 是否是 语音直播
    private boolean audioOnly;
    private String pin;
    private String host;
    private String title;
    private int action;
    private String targetUserId;
    // 是否是 语音连麦
    private boolean audioOnlyRequest;

    public LiveCoStreamContent() {
    }

    public LiveCoStreamContent(String callId, boolean audioOnly, String pin, String host, String title, int action, String targetUserId, boolean audioOnlyRequest) {
        this.callId = callId;
        this.audioOnly = audioOnly;
        this.pin = pin;
        this.host = host;
        this.title = title;
        this.action = action;
        this.targetUserId = targetUserId;
        this.audioOnlyRequest = audioOnlyRequest;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = callId;
        JSONObject json = new JSONObject();
        try {
            json.put("a", action);
            json.putOpt("p", pin);
            json.putOpt("au", audioOnly);
            json.putOpt("h", host);
            json.putOpt("ti", title);
            json.putOpt("u", targetUserId);
            json.putOpt("aur", audioOnlyRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        payload.binaryContent = json.toString().getBytes();
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        this.callId = payload.content;
        if (payload.binaryContent != null) {
            try {
                JSONObject json = new JSONObject(new String(payload.binaryContent));
                this.action = json.optInt("a");
                this.audioOnly = json.optBoolean("au");
                this.pin = json.optString("p");
                this.host = json.optString("h");
                this.title = json.optString("ti");
                this.targetUserId = json.optString("u");
                this.audioOnlyRequest = json.optBoolean("aur", audioOnlyRequest);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String digest(cn.wildfirechat.message.Message message) {
        return null;
    }

    public String getCallId() {
        return callId;
    }

    public boolean isAudioOnly() {
        return audioOnly;
    }

    public String getPin() {
        return pin;
    }

    public String getHost() {
        return host;
    }

    public String getTitle() {
        return title;
    }

    public int getAction() {
        return action;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public boolean isAudioOnlyRequest() {
        return audioOnlyRequest;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(callId);
        dest.writeInt(audioOnly ? 1 : 0);
        dest.writeString(pin);
        dest.writeString(host);
        dest.writeString(title);
        dest.writeInt(action);
        dest.writeString(targetUserId);
        dest.writeInt(audioOnlyRequest ? 1 : 0);
    }

    protected LiveCoStreamContent(Parcel in) {
        super(in);
        callId = in.readString();
        audioOnly = in.readInt() == 1;
        pin = in.readString();
        host = in.readString();
        title = in.readString();
        action = in.readInt();
        targetUserId = in.readString();
        audioOnlyRequest = in.readInt() == 1;
    }

    public static final Creator<LiveCoStreamContent> CREATOR = new Creator<LiveCoStreamContent>() {
        @Override
        public LiveCoStreamContent createFromParcel(Parcel source) {
            return new LiveCoStreamContent(source);
        }

        @Override
        public LiveCoStreamContent[] newArray(int size) {
            return new LiveCoStreamContent[size];
        }
    };
}
