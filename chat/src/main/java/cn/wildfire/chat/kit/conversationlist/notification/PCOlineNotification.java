package cn.wildfire.chat.kit.conversationlist.notification;

import android.view.View;

import androidx.fragment.app.Fragment;
import cn.wildfirechat.chat.R;

public class PCOlineNotification extends StatusNotification {
    public PCOlineNotification(Fragment fragment) {
        super(fragment);
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public int layoutRes() {
        return R.layout.conversationlist_item_notification_pc_online;
    }

    @Override
    public void onBind(View view, Object value) {

    }
}
