/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * 频道信息类
 * <p>
 * 用于表示频道的详细信息，包括频道ID、名称、头像、描述、所有者等基本信息。
 * 频道是野火IM中的一种特殊会话类型，支持订阅机制和权限控制。
 * 通过状态掩码可以控制频道的各种访问权限和功能特性。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class ChannelInfo implements Parcelable {
    /**
     * 频道状态掩码接口
     */
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

    /**
     * 频道ID
     */
    public String channelId;
    /**
     * 频道名称
     */
    public String name;
    /**
     * 频道头像URL
     */
    public String portrait;
    /**
     * 频道描述
     */
    public String desc;
    /**
     * 频道所有者ID
     */
    public String owner;
    /**
     * 频道状态（使用ChannelStatusMask中的掩码值）
     */
    public int status;
    /**
     * 额外信息
     */
    public String extra;
    /**
     * 更新时间戳
     */
    public long updateDt;
    /**
     * 频道菜单列表
     */
    public List<ChannelMenu> menus;

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
        dest.writeString(this.desc);
        dest.writeString(this.owner);
        dest.writeInt(this.status);
        dest.writeString(this.extra);
        dest.writeLong(this.updateDt);
        dest.writeList(this.menus);
    }

    public void readFromParcel(Parcel source) {
        this.channelId = source.readString();
        this.name = source.readString();
        this.portrait = source.readString();
        this.desc = source.readString();
        this.owner = source.readString();
        this.status = source.readInt();
        this.extra = source.readString();
        this.updateDt = source.readLong();
        this.menus = new ArrayList<ChannelMenu>();
        source.readList(this.menus, ChannelMenu.class.getClassLoader());
    }

    protected ChannelInfo(Parcel in) {
        this.channelId = in.readString();
        this.name = in.readString();
        this.portrait = in.readString();
        this.desc = in.readString();
        this.owner = in.readString();
        this.status = in.readInt();
        this.extra = in.readString();
        this.updateDt = in.readLong();
        this.menus = new ArrayList<ChannelMenu>();
        in.readList(this.menus, ChannelMenu.class.getClassLoader());
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
