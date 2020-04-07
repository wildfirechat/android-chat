package cn.wildfirechat.client;

public enum Platform {
    PlatformType_UNSET(0),
    PlatformType_iOS(1),
    PlatformType_Android(2),
    PlatformType_Windows(3),
    PlatformType_OSX(4),
    PlatformType_WEB(5),
    Platform_WX(6),
    Platform_Linux(7);

    private int value;

    Platform(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
