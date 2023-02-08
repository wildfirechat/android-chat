/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Employee implements Parcelable {
    public String employeeId;
    public int organizationId;
    public String name;
    public String title;
    public int level;
    public String mobile;
    public String email;
    public String ext;
    public String office;
    public String city;
    public String portraitUrl;
    public String jobNumber;
    public String joinTime;
    public int type;
    public int gender;
    public int sort;
    public long createDt;
    public long updateDt;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.employeeId);
        dest.writeInt(this.organizationId);
        dest.writeString(this.name);
        dest.writeString(this.title);
        dest.writeInt(this.level);
        dest.writeString(this.mobile);
        dest.writeString(this.email);
        dest.writeString(this.ext);
        dest.writeString(this.office);
        dest.writeString(this.city);
        dest.writeString(this.portraitUrl);
        dest.writeString(this.jobNumber);
        dest.writeString(this.joinTime);
        dest.writeInt(this.type);
        dest.writeInt(this.gender);
        dest.writeInt(this.sort);
        dest.writeLong(this.createDt);
        dest.writeLong(this.updateDt);
    }

    public void readFromParcel(Parcel source) {
        this.employeeId = source.readString();
        this.organizationId = source.readInt();
        this.name = source.readString();
        this.title = source.readString();
        this.level = source.readInt();
        this.mobile = source.readString();
        this.email = source.readString();
        this.ext = source.readString();
        this.office = source.readString();
        this.city = source.readString();
        this.portraitUrl = source.readString();
        this.jobNumber = source.readString();
        this.joinTime = source.readString();
        this.type = source.readInt();
        this.gender = source.readInt();
        this.sort = source.readInt();
        this.createDt = source.readLong();
        this.updateDt = source.readLong();
    }

    public Employee() {
    }

    protected Employee(Parcel in) {
        this.employeeId = in.readString();
        this.organizationId = in.readInt();
        this.name = in.readString();
        this.title = in.readString();
        this.level = in.readInt();
        this.mobile = in.readString();
        this.email = in.readString();
        this.ext = in.readString();
        this.office = in.readString();
        this.city = in.readString();
        this.portraitUrl = in.readString();
        this.jobNumber = in.readString();
        this.joinTime = in.readString();
        this.type = in.readInt();
        this.gender = in.readInt();
        this.sort = in.readInt();
        this.createDt = in.readLong();
        this.updateDt = in.readLong();
    }

    public static final Creator<Employee> CREATOR = new Creator<Employee>() {
        @Override
        public Employee createFromParcel(Parcel source) {
            return new Employee(source);
        }

        @Override
        public Employee[] newArray(int size) {
            return new Employee[size];
        }
    };
}
