/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 17/12/2017.
 */


public class DomainInfo implements Parcelable {
    public String domainId;
    public String name;
    public String email;
    public String desc;
    public String tel;
    public String address;
    public String extra;
    public long updateDt;

    public DomainInfo() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.domainId);
        dest.writeString(this.name);
        dest.writeString(this.email);
        dest.writeString(this.desc);
        dest.writeString(this.tel);
        dest.writeString(this.address);
        dest.writeString(this.extra);
        dest.writeLong(this.updateDt);
    }

    public void readFromParcel(Parcel source) {
        this.domainId = source.readString();
        this.name = source.readString();
        this.email = source.readString();
        this.desc = source.readString();
        this.tel = source.readString();
        this.address = source.readString();
        this.extra = source.readString();
        this.updateDt = source.readLong();
    }

    protected DomainInfo(Parcel in) {
        this.domainId = in.readString();
        this.name = in.readString();
        this.email = in.readString();
        this.desc = in.readString();
        this.tel = in.readString();
        this.address = in.readString();
        this.extra = in.readString();
        this.updateDt = in.readLong();
    }

    public static final Creator<DomainInfo> CREATOR = new Creator<DomainInfo>() {
        @Override
        public DomainInfo createFromParcel(Parcel source) {
            return new DomainInfo(source);
        }

        @Override
        public DomainInfo[] newArray(int size) {
            return new DomainInfo[size];
        }
    };
}
