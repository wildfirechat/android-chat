/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 群成员信息类
 * <p>
 * 表示群组成员的信息和角色。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class GroupMember implements Parcelable {
    /**
     * 群成员类型枚举
     */
    public enum GroupMemberType {
        /**
         * 普通成员
         */
        Normal(0),
        /**
         * 管理员
         */
        Manager(1),
        /**
         * 群主
         */
        Owner(2),
        /**
         * 被禁言成员
         */
        Muted(3),
        /**
         * 被移除成员
         */
        Removed(4),
        /**
         * 被允许成员（特殊权限）
         */
        Allowed(5);

        private int value;

        GroupMemberType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        /**
         * 根据值获取群成员类型
         *
         * @param type 群成员类型的值
         * @return 群成员类型枚举
         */
        public static GroupMemberType type(int type) {
            if (type >= 0 && type < GroupMemberType.values().length) {
                return GroupMemberType.values()[type];
            }

            throw new IllegalArgumentException("GroupMemberType " + type + " is invalid");
        }
    }

    /**
     * 群组ID
     */
    public String groupId;

    /**
     * 群成员ID
     */
    public String memberId;

    /**
     * 群成员别名/备注
     */
    public String alias;

    /**
     * 扩展字段
     */
    public String extra;

    /**
     * 群成员类型
     */
    public GroupMemberType type;

    /**
     * 更新时间戳
     */
    public long updateDt;

    /**
     * 创建时间戳
     */
    public long createDt;


    /**
     * 默认构造函数
     */
    public GroupMember() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.groupId);
        dest.writeString(this.memberId);
        dest.writeString(this.alias);
        dest.writeString(this.extra);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeLong(this.updateDt);
        dest.writeLong(this.createDt);
    }

    protected GroupMember(Parcel in) {
        this.groupId = in.readString();
        this.memberId = in.readString();
        this.alias = in.readString();
        this.extra = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : GroupMemberType.values()[tmpType];
        this.updateDt = in.readLong();
        this.createDt = in.readLong();
    }

    public static final Creator<GroupMember> CREATOR = new Creator<GroupMember>() {
        @Override
        public GroupMember createFromParcel(Parcel source) {
            return new GroupMember(source);
        }

        @Override
        public GroupMember[] newArray(int size) {
            return new GroupMember[size];
        }
    };
}
