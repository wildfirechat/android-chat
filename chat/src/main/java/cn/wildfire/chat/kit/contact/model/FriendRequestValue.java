package cn.wildfire.chat.kit.contact.model;

public class FriendRequestValue extends HeaderValue {
    private int unreadRequestCount;

    public FriendRequestValue(int unreadRequestCount) {
        this.unreadRequestCount = unreadRequestCount;
    }

    public int getUnreadRequestCount() {
        return unreadRequestCount;
    }

    public void setUnreadRequestCount(int unreadRequestCount) {
        this.unreadRequestCount = unreadRequestCount;
    }
}
