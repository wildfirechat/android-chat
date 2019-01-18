package cn.wildfirechat.message.core;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

public enum MessageStatus {
    Sending(0),
    Sent(1),
    Send_Failure(2),
    Mentioned(3),
    AllMentioned(4),
    Unread(5),
    Readed(6),
    Played(7);


    private int value;

    MessageStatus(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static MessageStatus status(int status) {
        MessageStatus messageStatus = null;
        switch (status) {
            case 0:
                messageStatus = Sending;
                break;
            case 1:
                messageStatus = Sent;
                break;
            case 2:
                messageStatus = Send_Failure;
                break;
            case 3:
                messageStatus = Mentioned;
                break;
            case 4:
                messageStatus = AllMentioned;
                break;
            case 5:
                messageStatus = Unread;
                break;
            case 6:
                messageStatus = Readed;
                break;
            case 7:
                messageStatus = Played;
                break;
            default:
                throw new IllegalArgumentException("status " + status + "is not valid");
        }
        return messageStatus;
    }
}
