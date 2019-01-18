package cn.wildfire.chat.conversationlist.notification;

import androidx.fragment.app.Fragment;
import android.view.View;

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
