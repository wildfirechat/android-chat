package cn.wildfirechat.message.core;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

public enum MessageDirection {
    Send(0),
    Receive(1);

    private int value;

    MessageDirection(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static MessageDirection direction(int direction) {
        MessageDirection messageDirection = null;
        switch (direction) {
            case 0:
                messageDirection = Send;
                break;
            case 1:
                messageDirection = Receive;
                break;
            default:
                throw new IllegalArgumentException("direction " + direction + " is invalid");
        }
        return messageDirection;

    }
}
