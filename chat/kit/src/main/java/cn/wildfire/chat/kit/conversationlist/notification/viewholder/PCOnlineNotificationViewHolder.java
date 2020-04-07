package cn.wildfire.chat.kit.conversationlist.notification.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import butterknife.BindView;
import cn.wildfire.chat.kit.annotation.LayoutRes;
import cn.wildfire.chat.kit.annotation.StatusNotificationType;
import cn.wildfire.chat.kit.conversationlist.notification.PCOnlineStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;
import cn.wildfirechat.chat.R;

@LayoutRes(resId = R.layout.conversationlist_item_notification_pc_online)
@StatusNotificationType(PCOnlineStatusNotification.class)
public class PCOnlineNotificationViewHolder extends StatusNotificationViewHolder {
    @BindView(R.id.statusTextView)
    TextView statusTextView;

    public PCOnlineNotificationViewHolder(Fragment fragment) {
        super(fragment);
    }

    @Override
    public void onBind(View view, StatusNotification notification) {
        PCOnlineStatusNotification pcOnlineStatusNotification = (PCOnlineStatusNotification) notification;
        String desc = "";
        switch (pcOnlineStatusNotification.getPcOnlineInfo().getType()) {
            case PC_Online:
                desc = "PC 在线";
                break;
            case Web_Online:
                desc = "Web 在线";
                break;
            case WX_Online:
                desc = "微信小程序 在线";
                break;
            default:
                break;
        }

        statusTextView.setText(desc);
    }
}
