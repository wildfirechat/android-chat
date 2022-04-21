/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import cn.wildfirechat.remote.ChatManager;

public class BurnMessageInfo implements Parcelable {
    private long messageId;
    private long messageUid;
    private String targetId;
    private int direction;
    private int isMedia;
    private int burnTime;
    private long messageDt;

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getIsMedia() {
        return isMedia;
    }

    public void setIsMedia(int isMedia) {
        this.isMedia = isMedia;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public long getMessageDt() {
        return messageDt;
    }

    public void setMessageDt(long messageDt) {
        this.messageDt = messageDt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(messageId);
        dest.writeLong(messageUid);
        dest.writeString(this.targetId);
        dest.writeInt(this.direction);
        dest.writeInt(this.isMedia);
        dest.writeInt(this.burnTime);
        dest.writeLong(this.messageDt);
    }

    public BurnMessageInfo() {
    }

    protected BurnMessageInfo(Parcel in) {
        this.messageId = in.readLong();
        this.messageUid = in.readLong();
        this.targetId = in.readString();
        this.direction = in.readInt();
        this.isMedia = in.readInt();
        this.burnTime = in.readInt();
        this.messageDt = in.readLong();
    }

    public static final Creator<BurnMessageInfo> CREATOR = new Creator<BurnMessageInfo>() {
        @Override
        public BurnMessageInfo createFromParcel(Parcel source) {
            return new BurnMessageInfo(source);
        }

        @Override
        public BurnMessageInfo[] newArray(int size) {
            return new BurnMessageInfo[size];
        }
    };
}
