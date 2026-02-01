/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 好友关系类
 * <p>
 * 表示当前用户与其他用户的好友关系。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class Friend implements Parcelable {
    /**
     * 好友用户ID
     */
    public String userId;

    /**
     * 好友备注
     */
    public String alias;

    /**
     * 扩展字段
     */
    public String extra;

    /**
     * 时间戳
     */
    public long timestamp;

    /**
     * 默认构造函数
     */
    public Friend() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.userId);
        dest.writeString(this.alias);
        dest.writeString(this.extra);
        dest.writeLong(this.timestamp);
    }

    protected Friend(Parcel in) {
        this.userId = in.readString();
        this.alias = in.readString();
        this.extra = in.readString();
        this.timestamp = in.readLong();
    }

    public static final Creator<Friend> CREATOR = new Creator<Friend>() {
        @Override
        public Friend createFromParcel(Parcel source) {
            return new Friend(source);
        }

        @Override
        public Friend[] newArray(int size) {
            return new Friend[size];
        }
    };
}
