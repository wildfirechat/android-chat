/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

/**
 * 修改群组信息类型枚举
 * <p>
 * 定义了可以修改的群组信息字段类型。
 * 包括群名称、群头像、附加信息、禁言设置、加入类型、私聊权限、搜索可见性、历史消息、最大成员数等。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public enum ModifyGroupInfoType {
    Modify_Group_Name(0),
    Modify_Group_Portrait(1),
    Modify_Group_Extra(2),
    Modify_Group_Mute(3),
    Modify_Group_JoinType(4),
    Modify_Group_PrivateChat(5),
    Modify_Group_Searchable(6),
    Modify_Group_History_Message(7), //仅专业版支持
    Modify_Group_Max_Member_Count(8);//仅专业版支持，仅server api可以修改


    private int value;

    ModifyGroupInfoType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ModifyGroupInfoType type(int type) {
        ModifyGroupInfoType out = null;
        if (type >= 0 && type < ModifyGroupInfoType.values().length) {
            return ModifyGroupInfoType.values()[type];
        }

        throw new IllegalArgumentException("ModifyMyInfoType " + type + " is invalid");

    }
}
