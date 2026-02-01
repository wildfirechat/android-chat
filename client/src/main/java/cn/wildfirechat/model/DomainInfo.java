/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 领域/租户信息类
 * <p>
 * 用于表示多租户系统中的领域（租户）信息。
 * 包含领域ID、名称、邮箱、描述、电话、地址等基本信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class DomainInfo implements Parcelable {
    /**
     * 领域ID
     */
    public String domainId;

    /**
     * 领域名称
     */
    public String name;

    /**
     * 联系邮箱
     */
    public String email;

    /**
     * 领域描述
     */
    public String desc;

    /**
     * 联系电话
     */
    public String tel;

    /**
     * 领域地址
     */
    public String address;

    /**
     * 附加信息
     */
    public String extra;

    /**
     * 更新时间
     */
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
