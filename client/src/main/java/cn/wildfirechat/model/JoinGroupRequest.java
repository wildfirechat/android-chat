/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 加入群组请求类
 * <p>
 * 用于表示申请加入群组的请求信息。
 * 包含群组ID、申请者、审批者、申请理由、请求状态等信息。
 * 支持加群请求的发送、接受、拒绝等操作。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class JoinGroupRequest implements Parcelable {
    /**
     * 群组ID
     */
    public String groupId;
    /**
     * 群成员ID
     */
    public String memberId;
    /**
     * 请求用户ID
     */
    public String requestUserId;
    /**
     * 审批用户ID
     */
    public String acceptUserId;
    /**
     * 申请理由
     */
    public String reason;
    /**
     * 额外信息
     */
    public String extra;
    /**
     * 请求状态：0-已发送，1-已接受，2-已拒绝
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

    public JoinGroupRequest() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.groupId);
        dest.writeString(this.memberId);
        dest.writeString(this.requestUserId);
        dest.writeString(this.acceptUserId);
        dest.writeString(this.reason);
        dest.writeString(this.extra);
        dest.writeInt(this.status);
        dest.writeInt(this.readStatus);
        dest.writeLong(this.timestamp);
    }

    protected JoinGroupRequest(Parcel in) {
        this.groupId = in.readString();
        this.memberId = in.readString();
        this.requestUserId = in.readString();
        this.acceptUserId = in.readString();
        this.reason = in.readString();
        this.extra = in.readString();
        this.status = in.readInt();
        this.readStatus = in.readInt();
        this.timestamp = in.readLong();
    }

    public static final Creator<JoinGroupRequest> CREATOR = new Creator<JoinGroupRequest>() {
        @Override
        public JoinGroupRequest createFromParcel(Parcel source) {
            return new JoinGroupRequest(source);
        }

        @Override
        public JoinGroupRequest[] newArray(int size) {
            return new JoinGroupRequest[size];
        }
    };
}
