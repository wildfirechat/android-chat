package cn.wildfire.chat.kit.conversationlist.notification.viewholder;

import android.view.View;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.annotation.LayoutRes;
import cn.wildfire.chat.kit.annotation.StatusNotificationType;
import cn.wildfire.chat.kit.conversationlist.notification.PCOnlineNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;
import cn.wildfirechat.chat.R;

@LayoutRes(resId = R.layout.conversationlist_item_notification_pc_online)
@StatusNotificationType(PCOnlineNotification.class)
public class PCOlineNotificationViewHolder extends StatusNotificationViewHolder {
    public PCOlineNotificationViewHolder(Fragment fragment) {
        super(fragment);
    }

    @Override
    public void onBind(View view, StatusNotification notification) {

    }
}
