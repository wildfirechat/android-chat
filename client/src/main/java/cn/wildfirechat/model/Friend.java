/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 14/12/2017.
 */

public class Friend implements Parcelable {
    public String userId;
    public String alias;
    public String extra;
    public long timestamp;

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
