/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Organization implements Parcelable {
    public int id;
    public int parentId;
    public String managerId;
    public String name;
    public String desc;
    public String portraitUrl;
    public String tel;
    public String office;
    public String groupId;
    public int memberCount;
    public int sort;
    public long updateDt;
    public long createDt;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeInt(this.parentId);
        dest.writeString(this.managerId);
        dest.writeString(this.name);
        dest.writeString(this.desc);
        dest.writeString(this.portraitUrl);
        dest.writeString(this.tel);
        dest.writeString(this.office);
        dest.writeString(this.groupId);
        dest.writeInt(this.memberCount);
        dest.writeInt(this.sort);
        dest.writeLong(this.updateDt);
        dest.writeLong(this.createDt);
    }

    public void readFromParcel(Parcel source) {
        this.id = source.readInt();
        this.parentId = source.readInt();
        this.managerId = source.readString();
        this.name = source.readString();
        this.desc = source.readString();
        this.portraitUrl = source.readString();
        this.tel = source.readString();
        this.office = source.readString();
        this.groupId = source.readString();
        this.memberCount = source.readInt();
        this.sort = source.readInt();
        this.updateDt = source.readLong();
        this.createDt = source.readLong();
    }

    public Organization() {
    }

    protected Organization(Parcel in) {
        this.id = in.readInt();
        this.parentId = in.readInt();
        this.managerId = in.readString();
        this.name = in.readString();
        this.desc = in.readString();
        this.portraitUrl = in.readString();
        this.tel = in.readString();
        this.office = in.readString();
        this.groupId = in.readString();
        this.memberCount = in.readInt();
        this.sort = in.readInt();
        this.updateDt = in.readLong();
        this.createDt = in.readLong();
    }

    public static final Creator<Organization> CREATOR = new Creator<Organization>() {
        @Override
        public Organization createFromParcel(Parcel source) {
            return new Organization(source);
        }

        @Override
        public Organization[] newArray(int size) {
            return new Organization[size];
        }
    };
}
