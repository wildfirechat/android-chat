package cn.wildfire.chat.kit.conversationlist.notification;

import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.kit.annotation.LayoutRes;
import cn.wildfire.chat.kit.annotation.StatusNotificationType;
import cn.wildfire.chat.kit.conversationlist.notification.viewholder.ConnectionNotificationViewHolder;
import cn.wildfire.chat.kit.conversationlist.notification.viewholder.PCOlineNotificationViewHolder;
import cn.wildfire.chat.kit.conversationlist.notification.viewholder.StatusNotificationViewHolder;

public class StatusNotificationManager {
    private static StatusNotificationManager instance;
    private Map<Class<? extends StatusNotification>, Class<? extends StatusNotificationViewHolder>> notificationViewHolders;

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
        registerNotificationViewHolder(PCOlineNotificationViewHolder.class);
        registerNotificationViewHolder(ConnectionNotificationViewHolder.class);
    }

    public void registerNotificationViewHolder(Class<? extends StatusNotificationViewHolder> holderClass) {
        StatusNotificationType notificationType = holderClass.getAnnotation(StatusNotificationType.class);
        LayoutRes layoutRes = holderClass.getAnnotation(LayoutRes.class);
        if (notificationType == null || layoutRes == null) {
            throw new IllegalArgumentException("missing annotation");
        }
        notificationViewHolders.put(notificationType.value(), holderClass);
    }

    public Class<? extends StatusNotificationViewHolder> getNotificationViewHolder(StatusNotification notification) {
        return notificationViewHolders.get(notification.getClass());
    }
}
