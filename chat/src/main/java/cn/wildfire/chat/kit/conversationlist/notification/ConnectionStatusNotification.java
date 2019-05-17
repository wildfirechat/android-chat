package cn.wildfire.chat.kit.conversationlist.notification;

public class ConnectionStatusNotification extends StatusNotification {
    private int status;
    private String value;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
