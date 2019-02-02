package cn.wildfire.chat.kit.conversationlist.notification;

import android.view.View;

import androidx.fragment.app.Fragment;

public abstract class StatusNotification {
    protected Fragment fragment;

    public StatusNotification(Fragment fragment) {
        this.fragment = fragment;
    }

    public abstract int priority();

    public abstract int layoutRes();

    /**
     * @param view the view inflate from {@link #layoutRes()}
     */
    public abstract void onBind(View view, Object value);

}
