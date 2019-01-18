package cn.wildfirechat.model;

/**
 * Created by heavyrainlee on 17/12/2017.
 */

public enum ModifyGroupInfoType {
    Modify_Group_Name(0),
    Modify_Group_Portrait(1),
    Modify_Group_Extra(2);

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
