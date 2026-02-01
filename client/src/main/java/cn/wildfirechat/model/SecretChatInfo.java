/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import cn.wildfirechat.remote.ChatManager;

/**
 * 秘密聊天信息类
 * <p>
 * 用于表示端到端加密聊天会话的信息。
 * 包含目标ID、用户ID、会话状态、阅后即焚时间和创建时间。
 * </p>
 *
 * @author WildFireChat
 * @since 2022
 */
public class SecretChatInfo implements Parcelable {
    /**
     * 目标用户ID
     */
    private String targetId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 秘密聊天状态
     */
    private ChatManager.SecretChatState state;

    /**
     * 阅后即焚时间（秒）
     */
    private int burnTime;

    /**
     * 会话创建时间
     */
    private long createTime;

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ChatManager.SecretChatState getState() {
        return state;
    }

    public void setState(ChatManager.SecretChatState state) {
        this.state = state;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.targetId);
        dest.writeString(this.userId);
        dest.writeInt(this.state.ordinal());
        dest.writeInt(this.burnTime);
        dest.writeLong(this.createTime);
    }

    public SecretChatInfo() {
    }

    protected SecretChatInfo(Parcel in) {
        this.targetId = in.readString();
        this.userId = in.readString();
        this.state = ChatManager.SecretChatState.fromValue(in.readInt());
        this.burnTime = in.readInt();
        this.createTime = in.readLong();
    }

    public static final Creator<SecretChatInfo> CREATOR = new Creator<SecretChatInfo>() {
        @Override
        public SecretChatInfo createFromParcel(Parcel source) {
            return new SecretChatInfo(source);
        }

        @Override
        public SecretChatInfo[] newArray(int size) {
            return new SecretChatInfo[size];
        }
    };
}
