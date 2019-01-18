package cn.wildfirechat.model;

/**
 * Created by heavyrainlee on 17/12/2017.
 */

public enum ModifyMyInfoType {
    Modify_DisplayName(0),
    Modify_Portrait(1),
    //性别属性是int类型，修改时转换成string类型
    Modify_Gender(2),
    Modify_Mobile(3),
    Modify_Email(4),
    Modify_Address(5),
    Modify_Company(6),
    Modify_Social(7),
    Modify_Extra(8);

    private int value;

    ModifyMyInfoType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ModifyMyInfoType type(int type) {
        ModifyMyInfoType out = null;
        if (type >= 0 && type < ModifyMyInfoType.values().length) {
            return ModifyMyInfoType.values()[type];
        }

        throw new IllegalArgumentException("ModifyMyInfoType " + type + " is invalid");

    }
}
