/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.model;

public class JoinGroupRequestValue extends HeaderValue {
    private int unreadRequestCount;

    public JoinGroupRequestValue(int unreadRequestCount) {
        this.unreadRequestCount = unreadRequestCount;
    }

    public int getUnreadRequestCount() {
        return unreadRequestCount;
    }

    public void setUnreadRequestCount(int unreadRequestCount) {
        this.unreadRequestCount = unreadRequestCount;
    }

}
