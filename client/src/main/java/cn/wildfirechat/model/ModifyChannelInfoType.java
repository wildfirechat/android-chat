package cn.wildfirechat.model;

/**
 * Created by heavyrainlee on 17/12/2017.
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
