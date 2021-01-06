/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 17/12/2017.
 */


public class ChannelInfo implements Parcelable {
    public interface ChannelStatusMask {
        //第0位表示是否允许查看用户所有信息，还是只允许看用户id，用户名称，用户昵称和用户头像
        int Channel_State_Mask_FullInfo = 0x01;
        //第1位表示是否允许查看非订阅用户信息
        int Channel_State_Mask_Unsubscribed_User_Access = 0x02;
        //第2位表示是否允许主动添加用户订阅关系
        int Channel_State_Mask_Active_Subscribe = 0x04;
        //第3位表示是否允许给非订阅用户发送消息
        int Channel_State_Mask_Message_Unsubscribed = 0x08;
        //第4位表示是否私有
        int Channel_State_Mask_Private = 0x10;
        //第6位表示是否删除
        int Channel_State_Mask_Deleted = 0x40;
    }

    public String channelId;
    public String name;
    public String portrait;
    public String desc;
    public String owner;
    public int status;
    public String extra;
    public long updateDt;

    public ChannelInfo() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.channelId);
        dest.writeString(this.name);
        dest.writeString(this.portrait);
        dest.writeString(this.owner);
        dest.writeString(this.desc);
        dest.writeInt(this.status);
        dest.writeString(this.extra);
        dest.writeLong(this.updateDt);
    }

    protected ChannelInfo(Parcel in) {
        this.channelId = in.readString();
        this.name = in.readString();
        this.portrait = in.readString();
        this.owner = in.readString();
        this.desc = in.readString();
        this.status = in.readInt();
        this.extra = in.readString();
        this.updateDt = in.readLong();
    }

    public static final Creator<ChannelInfo> CREATOR = new Creator<ChannelInfo>() {
        @Override
        public ChannelInfo createFromParcel(Parcel source) {
            return new ChannelInfo(source);
        }

        @Override
        public ChannelInfo[] newArray(int size) {
            return new ChannelInfo[size];
        }
    };
}
