package cn.wildfirechat.message;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

public enum MessageContentMediaType {
    GENERAL(0),
    IMAGE(1),
    VOICE(2),
    VIDEO(3),
    FILE(4),
    PORTRAIT(5),
    FAVORITE(6);

    private int value;

    MessageContentMediaType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MessageContentMediaType mediaType(int mediaType) {
        MessageContentMediaType type = null;
        if (mediaType >= 0 && mediaType < MessageContentMediaType.values().length) {
            return MessageContentMediaType.values()[mediaType];
        }
        return null;
        //throw new IllegalArgumentException("mediaType " + mediaType + " is invalid");
    }
}
