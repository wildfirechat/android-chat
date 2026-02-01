/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 好友请求类
 * <p>
 * 用于表示好友请求的信息，包括发送的好友请求和接收的好友请求。
 * 包含请求方向、目标用户、申请理由、请求状态等信息。
 * 支持好友请求的发送、接受、拒绝等操作。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class FriendRequest implements Parcelable {
    /**
     * 请求方向：0-发送，1-接收
     */
    public int direction;
    /**
     * 目标用户ID
     */
    public String target;
    /**
     * 申请理由
     */
    public String reason;
    /**
     * 额外信息
     */
    public String extra;
    /**
     * 请求状态：0-已发送，1-已接受，3-已拒绝
     */
    public int status;
    /**
     * 已读状态
     */
    public int readStatus;
    /**
     * 时间戳
     */
    public long timestamp;

    public FriendRequest() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.direction);
        dest.writeString(this.target);
        dest.writeString(this.reason);
        dest.writeString(this.extra);
        dest.writeInt(this.status);
        dest.writeInt(this.readStatus);
        dest.writeLong(this.timestamp);
    }

    protected FriendRequest(Parcel in) {
        this.direction = in.readInt();
        this.target = in.readString();
        this.reason = in.readString();
        this.extra = in.readString();
        this.status = in.readInt();
        this.readStatus = in.readInt();
        this.timestamp = in.readLong();
    }

    public static final Creator<FriendRequest> CREATOR = new Creator<FriendRequest>() {
        @Override
        public FriendRequest createFromParcel(Parcel source) {
            return new FriendRequest(source);
        }

        @Override
        public FriendRequest[] newArray(int size) {
            return new FriendRequest[size];
        }
    };
}
