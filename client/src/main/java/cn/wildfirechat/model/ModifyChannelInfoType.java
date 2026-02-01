/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

/**
 * 修改频道信息类型枚举
 * <p>
 * 定义了可以修改的频道信息字段类型。
 * 包括频道名称、频道头像、频道描述、频道附加信息等。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public enum ModifyChannelInfoType {
    Modify_Channel_Name(0),
    Modify_Channel_Portrait(1),
    Modify_Channel_Desc(2),
    Modify_Channel_Extra(3);

    private int value;

    ModifyChannelInfoType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ModifyChannelInfoType type(int type) {
        ModifyChannelInfoType out = null;
        if (type >= 0 && type < ModifyChannelInfoType.values().length) {
            return ModifyChannelInfoType.values()[type];
        }

        throw new IllegalArgumentException("ModifyMyInfoType " + type + " is invalid");

    }
}
