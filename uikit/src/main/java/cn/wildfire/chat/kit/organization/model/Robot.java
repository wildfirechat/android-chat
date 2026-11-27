/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.model;

import android.os.Parcel;
import android.os.Parcelable;

import cn.wildfirechat.model.UserInfo;

/**
 * 组织架构机器人（按公司维护，客户端只读）。
 * <p>
 * 列表由组织通讯录服务下发，顺序即服务端维护的顺序，客户端不再排序。
 */
public class Robot implements Parcelable {
    public int companyId;
    public String robotId;
    public String robotName;
    public String portraitUrl;
    public int sort;
    public long createdAt;

    /**
     * 机器人本身不在 IM 好友/群成员体系里，选人结果统一用它转成 {@link UserInfo}
     * 交给原有选人回调，因此 {@code uid} 就是机器人 userId。
     */
    public UserInfo toUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.uid = robotId;
        String name = (robotName == null || robotName.isEmpty()) ? robotId : robotName;
        userInfo.name = name;
        userInfo.displayName = name;
        userInfo.portrait = portraitUrl;
        // 0 normal; 1 robot; 2 thing;
        userInfo.type = 1;
        userInfo.deleted = 0;
        return userInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.companyId);
        dest.writeString(this.robotId);
        dest.writeString(this.robotName);
        dest.writeString(this.portraitUrl);
        dest.writeInt(this.sort);
        dest.writeLong(this.createdAt);
    }

    public void readFromParcel(Parcel source) {
        this.companyId = source.readInt();
        this.robotId = source.readString();
        this.robotName = source.readString();
        this.portraitUrl = source.readString();
        this.sort = source.readInt();
        this.createdAt = source.readLong();
    }

    public Robot() {
    }

    protected Robot(Parcel in) {
        this.companyId = in.readInt();
        this.robotId = in.readString();
        this.robotName = in.readString();
        this.portraitUrl = in.readString();
        this.sort = in.readInt();
        this.createdAt = in.readLong();
    }

    public static final Creator<Robot> CREATOR = new Creator<Robot>() {
        @Override
        public Robot createFromParcel(Parcel source) {
            return new Robot(source);
        }

        @Override
        public Robot[] newArray(int size) {
            return new Robot[size];
        }
    };
}
