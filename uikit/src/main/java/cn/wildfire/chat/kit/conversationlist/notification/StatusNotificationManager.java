package cn.wildfire.chat.kit.conversationlist.notification;

import androidx.annotation.LayoutRes;

import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.kit.annotation.StatusNotificationType;
import cn.wildfire.chat.kit.conversationlist.notification.viewholder.ConnectionNotificationViewHolder;
import cn.wildfire.chat.kit.conversationlist.notification.viewholder.PCOnlineNotificationViewHolder;
import cn.wildfire.chat.kit.conversationlist.notification.viewholder.StatusNotificationViewHolder;
import cn.wildfire.chat.kit.R;

public class StatusNotificationManager {
    private static StatusNotificationManager instance;
    private Map<Class<? extends StatusNotification>, Class<? extends StatusNotificationViewHolder>> notificationViewHolders;
    private Map<Class<? extends StatusNotification>, Integer> notificationViewHolderLayoutResMap;

    public synchronized static StatusNotificationManager getInstance() {
        if (instance == null) {
            instance = new StatusNotificationManager();
        }
        return instance;
    }

    private StatusNotificationManager() {
        init();
    }

    private void init() {
        notificationViewHolders = new HashMap<>();
        notificationViewHolderLayoutResMap = new HashMap<>();
        registerNotificationViewHolder(PCOnlineNotificationViewHolder.class, R.layout.conversationlist_item_notification_pc_online);
        registerNotificationViewHolder(ConnectionNotificationViewHolder.class, R.layout.conversationlist_item_notification_connection_status);
    }

    public void registerNotificationViewHolder(Class<? extends StatusNotificationViewHolder> holderClass, @LayoutRes int layoutResId) {
        StatusNotificationType notificationType = holderClass.getAnnotation(StatusNotificationType.class);
        notificationViewHolders.put(notificationType.value(), holderClass);
        notificationViewHolderLayoutResMap.put(notificationType.value(), layoutResId);
    }

    public Class<? extends StatusNotificationViewHolder> getNotificationViewHolder(StatusNotification notification) {
        return notificationViewHolders.get(notification.getClass());
    }

    public @LayoutRes
    int getNotificationViewHolderLayoutResId(StatusNotification notification) {
        return notificationViewHolderLayoutResMap.get(notification.getClass());
    }
}
