/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.client;

public enum Platform {
    PlatformType_UNSET(0),
    PlatformType_iOS(1),
    PlatformType_Android(2),
    PlatformType_Windows(3),
    PlatformType_OSX(4),
    PlatformType_WEB(5),
    PlatformType_WX(6),
    PlatformType_Linux(7),
    PlatformType_iPad(8),
    PlatformType_APad(9),
    PlatformType_Harmony(10),
    PlatformType_Harmony_Pad(11),
    PlatformType_Harmony_PC(12);

    private int value;

    Platform(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static Platform platform(int platform) {
        if (platform >= 0 && platform < 13) {
            return Platform.values()[platform];
        }
        return Platform.PlatformType_UNSET;
    }

    public String getPlatFormName() {
        String platFormName = "PC";
        switch (this) {
            case PlatformType_iOS:
                platFormName = "iOS";
                break;
            case PlatformType_Android:
                platFormName = "Android";
                break;
            case PlatformType_Windows:
                platFormName = "Windows";
                break;
            case PlatformType_OSX:
                platFormName = "Mac";
                break;
            case PlatformType_Linux:
                platFormName = "Linux";
                break;
            case PlatformType_WEB:
                platFormName = "Web";
                break;
            case PlatformType_WX:
                platFormName = "小程序";
                break;
            case PlatformType_iPad:
                platFormName = "iPad";
                break;
            case PlatformType_APad:
                platFormName = "Android 平板";
                break;
            case PlatformType_Harmony:
                platFormName = "鸿蒙手机";
                break;
            case PlatformType_Harmony_Pad:
                platFormName = "鸿蒙Pad";
                break;
            case PlatformType_Harmony_PC:
                platFormName = "鸿蒙PC";
                break;
            default:
                break;
        }
        return platFormName;
    }
}
