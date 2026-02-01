/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * 用户信息类
 * <p>
 * 表示IM系统中的用户信息，包含用户的基本资料和扩展信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class UserInfo implements Parcelable, Comparable<UserInfo> {
    /**
     * 用户ID
     */
    public String uid;

    /**
     * 用户名
     */
    public String name;

    /**
     * 显示名称
     */
    public String displayName;

    /**
     * 用户在群里面给自己设置的备注，不同群不一样
     */
    public String groupAlias;

    /**
     * 我为好友设置的备注
     */
    public String friendAlias;

    /**
     * 用户头像URL
     */
    public String portrait;

    /**
     * 性别：0 未知；1 男；2 女
     */
    public int gender;

    /**
     * 手机号
     */
    public String mobile;

    /**
     * 邮箱
     */
    public String email;

    /**
     * 地址
     */
    public String address;

    /**
     * 公司
     */
    public String company;

    /**
     * 社交信息
     */
    public String social;

    /**
     * 扩展字段
     */
    public String extra;

    /**
     * 更新时间戳
     */
    public long updateDt;

    /**
     * 用户类型：0 普通用户；1 机器人；2 IoT设备
     */
    public int type;

    /**
     * 删除状态：0 正常；1 已删除
     */
    public int deleted;

    /**
     * 默认构造函数
     */
    public UserInfo() {
    }


    @Override
    public int compareTo(@NonNull UserInfo userInfo) {
        return displayName.compareTo(userInfo.displayName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uid);
        dest.writeString(this.name);
        dest.writeString(this.displayName);
        dest.writeString(this.groupAlias);
        dest.writeString(this.friendAlias);
        dest.writeString(this.portrait);
        dest.writeInt(this.gender);
        dest.writeString(this.mobile);
        dest.writeString(this.email);
        dest.writeString(this.address);
        dest.writeString(this.company);
        dest.writeString(this.social);
        dest.writeString(this.extra);
        dest.writeLong(this.updateDt);
        dest.writeInt(this.type);
        dest.writeInt(this.deleted);
    }

    protected UserInfo(Parcel in) {
        this.uid = in.readString();
        this.name = in.readString();
        this.displayName = in.readString();
        this.groupAlias = in.readString();
        this.friendAlias = in.readString();
        this.portrait = in.readString();
        this.gender = in.readInt();
        this.mobile = in.readString();
        this.email = in.readString();
        this.address = in.readString();
        this.company = in.readString();
        this.social = in.readString();
        this.extra = in.readString();
        this.updateDt = in.readLong();
        this.type = in.readInt();
        this.deleted = in.readInt();
    }

    public static final Creator<UserInfo> CREATOR = new Creator<UserInfo>() {
        @Override
        public UserInfo createFromParcel(Parcel source) {
            return new UserInfo(source);
        }

        @Override
        public UserInfo[] newArray(int size) {
            return new UserInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserInfo userInfo = (UserInfo) o;

        if (updateDt != userInfo.updateDt) return false;
        if (type != userInfo.type) return false;
        return uid.equals(userInfo.uid);
    }

    @Override
    public int hashCode() {
        int result = uid.hashCode();
        result = 31 * result + (int) (updateDt ^ (updateDt >>> 32));
        result = 31 * result + type;
        return result;
    }
}
