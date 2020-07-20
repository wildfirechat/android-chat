package cn.wildfire.chat.kit.conversationlist.notification;

import androidx.annotation.Nullable;

public abstract class StatusNotification {
    @Override
    public boolean equals(@Nullable Object obj) {
        return this.getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
