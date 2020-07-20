package cn.wildfire.chat.kit.conversationlist.notification.viewholder;

import android.view.View;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;

public abstract class StatusNotificationViewHolder {
    protected Fragment fragment;

    public StatusNotificationViewHolder(Fragment fragment) {
        this.fragment = fragment;
    }

    public abstract void onBind(View view, StatusNotification notification);
}
