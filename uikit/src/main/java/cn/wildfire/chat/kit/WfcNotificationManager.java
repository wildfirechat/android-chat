/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import static androidx.core.app.NotificationCompat.CATEGORY_MESSAGE;
import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static cn.wildfirechat.message.core.PersistFlag.Persist_And_Count;
import static cn.wildfirechat.model.Conversation.ConversationType.Single;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.contact.newfriend.FriendRequestListActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetUserInfoCallback;

public class WfcNotificationManager {
    private WfcNotificationManager() {

    }

    private static WfcNotificationManager notificationManager;
    // 小米系统上，允许推送时，默认还是没有铃声的。将 wfcChannelId 设置为接入推送时，在小米后台申请的 channelId，可以保证铃声生效
    private static final String wfcNotificationChannelId = "wfc_notification";

    private final List<Long> notificationMessages = new ArrayList<>();
    private int friendRequestNotificationId = 10000;

    public synchronized static WfcNotificationManager getInstance() {
        if (notificationManager == null) {
            notificationManager = new WfcNotificationManager();
        }
        return notificationManager;
    }

    public void clearAllNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        notificationMessages.clear();
    }

    private void showNotification(Context context, String tag, int id, String title, String content, PendingIntent pendingIntent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri notificationRingUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.receive_msg_notification);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(wfcNotificationChannelId,
                "野火IM 消息通知",
                NotificationManager.IMPORTANCE_HIGH);

            channel.enableLights(true); //是否在桌面icon右上角展示小红点
            channel.setLightColor(Color.GREEN); //小红点颜色
            channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
//            Uri defaultNotificaitonSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes.Builder builder = new AudioAttributes.Builder();
            builder
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            // 小米手机，发送消息时设置自定义铃声将不会生效，需要在新建 Channel 时设置自定义铃声。
            // 请参考：https://dev.mi.com/console/doc/detail?pId=2422#_2
            channel.setSound(notificationRingUri, builder.build());
            notificationManager.createNotificationChannel(channel);
        }


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, wfcNotificationChannelId)
            .setSmallIcon(R.mipmap.ic_launcher_notification)
            .setAutoCancel(true)
            .setCategory(CATEGORY_MESSAGE)
            .setSound(notificationRingUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(DEFAULT_ALL);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        builder.setContentText(content);

        notificationManager.notify(tag, id, builder.build());
    }

    public void handleRecallMessage(Context context, Message message) {
        handleReceiveMessage(context, Collections.singletonList(message));
    }

    public void handleDeleteMessage(Context context, Message message) {
        if (notificationMessages.contains(message.messageUid)) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationMessages.indexOf(message.messageUid));
        }
    }

    public void handleReceiveMessage(Context context, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        if (ChatManager.Instance().isNoDisturbing()) {
            return;
        }

        if (ChatManager.Instance().isGlobalSilent()) {
            return;
        }

        if (ChatManager.Instance().isMuteNotificationWhenPcOnline()) {
            for (PCOnlineInfo onlineInfo : ChatManager.Instance().getPCOnlineInfos()) {
                if (onlineInfo.isOnline()) {
                    return;
                }
            }
        }

        boolean hiddenNotificationDetail = ChatManager.Instance().isHiddenNotificationDetail();

        for (Message message : messages) {
            if (message.direction == MessageDirection.Send || (message.content.getPersistFlag() != Persist_And_Count && !(message.content instanceof RecallMessageContent))) {
                continue;
            }
            // 朋友圈取消点赞
            if (message.conversation.line == 1 && message.content.getMessageContentType() == MessageContentType.ContentType_Recall) {
                continue;
            }
            ConversationInfo conversationInfo = ChatManager.Instance().getConversation(message.conversation);
            if (conversationInfo != null && conversationInfo.isSilent) {
                continue;
            }

            String pushContent = hiddenNotificationDetail ? "新消息" : message.content.pushContent;
            if (TextUtils.isEmpty(pushContent)) {
                pushContent = message.content.digest(message);
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
                title = groupInfo == null ? "群聊" : (!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name);
            } else if (message.conversation.type == Conversation.ConversationType.Channel) {
                ChannelInfo channelInfo = ChatManager.Instance().getChannelInfo(message.conversation.target, false);
                title = channelInfo == null ? "公众号新消息" : channelInfo.name;
            } else {
                title = "新消息";
            }
            Intent mainIntent = new Intent(context.getPackageName() + ".main");
            Intent conversationIntent = new Intent(context, ConversationActivity.class);
            conversationIntent.putExtra("conversation", message.conversation);

            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= 23) {
                pendingIntent = PendingIntent.getActivities(context, notificationId(message.messageUid), new Intent[]{mainIntent, conversationIntent}, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivities(context, notificationId(message.messageUid), new Intent[]{mainIntent, conversationIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            String tag = "wfc notification tag";
            showNotification(context, tag, notificationId(message.messageUid), title, pushContent, pendingIntent);
        }
    }

    public void handleFriendRequest(Context context, List<String> friendRequests) {

        if (ChatManager.Instance().isGlobalSilent()) {
            return;
        }
        ChatManager.Instance().getUserInfo(friendRequests.get(0), true, new GetUserInfoCallback() {
            @Override
            public void onSuccess(UserInfo userInfo) {
                String text = userInfo.displayName;
                if (friendRequests.size() > 1) {
                    text += " 等";
                }
                text += "请求添加你为好友";
                String title = "好友申请";
                showFriendRequestNotification(context, title, text);
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    private void showFriendRequestNotification(Context context, String title, String text) {
        Intent mainIntent = new Intent(context.getPackageName() + ".main");
        Intent friendRequestListIntent = new Intent(context, FriendRequestListActivity.class);
        friendRequestNotificationId++;
        PendingIntent pendingIntent = PendingIntent.getActivities(context, friendRequestNotificationId, new Intent[]{mainIntent, friendRequestListIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        String tag = "wfc friendRequest notification tag";
        showNotification(context, tag, friendRequestNotificationId, title, text, pendingIntent);

    }

    private int notificationId(long messageUid) {
        if (!notificationMessages.contains(messageUid)) {
            notificationMessages.add(messageUid);
        }
        return notificationMessages.indexOf(messageUid);
    }
}
