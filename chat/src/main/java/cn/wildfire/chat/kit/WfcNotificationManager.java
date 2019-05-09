package cn.wildfire.chat.kit;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

import static androidx.core.app.NotificationCompat.CATEGORY_MESSAGE;
import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static cn.wildfirechat.message.core.PersistFlag.Persist_And_Count;
import static cn.wildfirechat.model.Conversation.ConversationType.Single;

public class WfcNotificationManager {
    private WfcNotificationManager() {

    }

    private static WfcNotificationManager notificationManager;

    public synchronized static WfcNotificationManager getInstance() {
        if (notificationManager == null) {
            notificationManager = new WfcNotificationManager();
        }
        return notificationManager;
    }

    public void clearAllNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        notificationConversations.clear();
    }

    private void showNotification(Context context, String tag, int id, String title, String content, PendingIntent pendingIntent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "wfc_notification";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "wildfire chat message",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.enableLights(true); //是否在桌面icon右上角展示小红点
            channel.setLightColor(Color.GREEN); //小红点颜色
            channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
            notificationManager.createNotificationChannel(channel);
        }


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setCategory(CATEGORY_MESSAGE)
                .setDefaults(DEFAULT_ALL);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        builder.setContentText(content);

        notificationManager.notify(tag, id, builder.build());
    }

    public void handleRecallMessage(Context context, Message message) {
        handleReceiveMessage(context, Collections.singletonList(message));
    }

    public void handleReceiveMessage(Context context, List<Message> messages) {

        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (Message message : messages) {
            if (message.direction == MessageDirection.Send || (message.content.getPersistFlag() != Persist_And_Count && !(message.content instanceof RecallMessageContent))) {
                continue;
            }
            String pushContent = message.content.encode().pushContent;
            if (TextUtils.isEmpty(pushContent)) {
                if (message.content.getType() == MessageContentType.ContentType_Text) {
                    TextMessageContent textMessageContent = (TextMessageContent) message.content;
                    pushContent = textMessageContent.getContent();
                } else if (message.content.getType() == MessageContentType.ContentType_Image) {
                    pushContent = "[图片]";
                } else if (message.content.getType() == MessageContentType.ContentType_Voice) {
                    pushContent = "[语音]";
                } else if (message.content.getType() == MessageContentType.ContentType_Recall) {
                    pushContent = "撤回了一条消息";
                }
            }

            int unreadCount = ChatManager.Instance().getUnreadCount(message.conversation).unread;
            if (unreadCount > 1) {
                pushContent = "[" + unreadCount + "条]" + pushContent;
            }

            String title = "";
            if (message.conversation.type == Single) {
                String name = ChatManager.Instance().getUserDisplayName(message.conversation.target);
                title = TextUtils.isEmpty(name) ? "新消息" : name;
            } else if (message.conversation.type == Conversation.ConversationType.Group) {
                GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(message.conversation.target, false);
                title = groupInfo == null ? "群聊" : groupInfo.name;
            } else {
                title = "新消息";
            }
            Intent mainIntent = new Intent(context, MainActivity.class);
            Intent conversationIntent = new Intent(context, ConversationActivity.class);
            conversationIntent.putExtra("conversation", message.conversation);
            PendingIntent pendingIntent = PendingIntent.getActivities(context, notificationId(message.conversation), new Intent[]{mainIntent, conversationIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
            String tag = "wfc notification tag";
            showNotification(context, tag, notificationId(message.conversation), title, pushContent, pendingIntent);
        }
    }

    private List<Conversation> notificationConversations = new ArrayList<>();

    private int notificationId(Conversation conversation) {
        if (!notificationConversations.contains(conversation)) {
            notificationConversations.add(conversation);
        }
        return notificationConversations.indexOf(conversation);
    }
}
