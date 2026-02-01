/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 群组信息类
 * <p>
 * 表示IM系统中的群组信息，包含群组的基本资料和设置。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class GroupInfo implements Parcelable {
    /**
     * 群组类型枚举
     */
    public enum GroupType {
        /**
         * 普通群：成员可以添加、退出、修改群名和头像，群主可以执行所有操作
         */
        Normal(0),
        /**
         * 自由群：每个成员都可以添加、退出、修改群名和头像，没有人可以踢出其他人
         */
        Free(1),
        /**
         * 受限群：成员只能退出，群主可以执行所有操作
         */
        Restricted(2),
        /**
         * 组织群：成员不能做任何操作，由服务器API管理群组
         */
        Organization(3);

        private int value;

        GroupType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        /**
         * 根据值获取群组类型
         *
         * @param type 群组类型的值
         * @return 群组类型枚举
         */
        public static GroupType type(int type) {
            if (type >= 0 && type < GroupType.values().length) {
                return GroupType.values()[type];
            }

            throw new IllegalArgumentException("GroupType " + type + " is invalid");
        }
    }

    /**
     * 群组ID
     */
    public String target;

    /**
     * 群组名称
     */
    public String name;

    /**
     * 群组头像URL
     */
    public String portrait;

    /**
     * 群主用户ID
     */
    public String owner;

    /**
     * 群组类型
     */
    public GroupType type;

    /**
     * 群成员数量
     */
    public int memberCount;

    /**
     * 扩展字段
     */
    public String extra;

    /**
     * 群备注
     */
    public String remark;

    /**
     * 更新时间戳
     */
    public long updateDt;

    /**
     * 群成员的最后更新日期
     * < -1 已退出群组；-1 未加入群组；> -1 已加入群组
     */
    public long memberDt;

    /**
     * 禁言状态：0 正常；1 全局禁言
     */
    public int mute;

    /**
     * 加群权限（群类型为Restricted时有效）：
     * 0 开放加入权限（群成员可以拉人，用户也可以主动加入）；
     * 1 只能群成员拉人入群；
     * 2 只能群管理拉人入群；
     * 3 加群需要验证
     */
    public int joinType;

    /**
     * 是否允许群中普通成员私聊：0 允许；1 不允许
     */
    public int privateChat;

    /**
     * 是否可以搜索到该群：0 群可以被搜索到；1 群不会被搜索到
     */
    public int searchable;

    /**
     * 群成员是否可以加载加入之前的历史消息：0 不可以；1 可以（仅专业版有效）
     */
    public int historyMessage;

    /**
     * 群最大成员数（仅专业版有效）
     */
    public int maxMemberCount;

    /**
     * 是否是超级群组：0 普通群组；1 超级群组（超级群组不支持服务器端删除）
     */
    public int superGroup;

    /**
     * 群组是否被解散：0 没有被解散；1 已经被解散
     */
    public int deleted;

    /**
     * 默认构造函数
     */
    public GroupInfo() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.target);
        dest.writeString(this.name);
        dest.writeString(this.portrait);
        dest.writeString(this.owner);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeInt(this.memberCount);
        dest.writeString(this.extra);
        dest.writeString(this.remark);
        dest.writeLong(this.updateDt);
        dest.writeLong(this.memberDt);
        dest.writeInt(this.mute);
        dest.writeInt(this.joinType);
        dest.writeInt(this.privateChat);
        dest.writeInt(this.searchable);
        dest.writeInt(this.historyMessage);
        dest.writeInt(this.maxMemberCount);
        dest.writeInt(this.superGroup);
        dest.writeInt(this.deleted);
    }

    protected GroupInfo(Parcel in) {
        this.target = in.readString();
        this.name = in.readString();
        this.portrait = in.readString();
        this.owner = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : GroupType.values()[tmpType];
        this.memberCount = in.readInt();
        this.extra = in.readString();
        this.remark = in.readString();
        this.updateDt = in.readLong();
        this.memberDt = in.readLong();
        this.mute = in.readInt();
        this.joinType = in.readInt();
        this.privateChat = in.readInt();
        this.searchable = in.readInt();
        this.historyMessage = in.readInt();
        this.maxMemberCount = in.readInt();
        this.superGroup = in.readInt();
        this.deleted = in.readInt();
    }

    public static final Creator<GroupInfo> CREATOR = new Creator<GroupInfo>() {
        @Override
        public GroupInfo createFromParcel(Parcel source) {
            return new GroupInfo(source);
        }

        @Override
        public GroupInfo[] newArray(int size) {
            return new GroupInfo[size];
        }
    };
}
