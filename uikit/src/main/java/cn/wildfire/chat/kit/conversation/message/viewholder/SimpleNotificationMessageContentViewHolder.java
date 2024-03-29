/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.chatme.message.AddParticipantsMessageContent;
import cn.chatme.message.notification.AddGroupMemberNotificationContent;
import cn.chatme.message.notification.ChangeGroupNameNotificationContent;
import cn.chatme.message.notification.ChangeGroupPortraitNotificationContent;
import cn.chatme.message.notification.CreateGroupNotificationContent;
import cn.chatme.message.notification.DismissGroupNotificationContent;
import cn.chatme.message.notification.FriendAddedMessageContent;
import cn.chatme.message.notification.FriendGreetingMessageContent;
import cn.chatme.message.notification.GroupAllowMemberNotificationContent;
import cn.chatme.message.notification.GroupJoinTypeNotificationContent;
import cn.chatme.message.notification.GroupMuteMemberNotificationContent;
import cn.chatme.message.notification.GroupMuteNotificationContent;
import cn.chatme.message.notification.GroupPrivateChatNotificationContent;
import cn.chatme.message.notification.GroupSetManagerNotificationContent;
import cn.chatme.message.notification.KickoffGroupMemberNotificationContent;
import cn.chatme.message.notification.KickoffGroupMemberVisibleNotificationContent;
import cn.chatme.message.notification.ModifyGroupAliasNotificationContent;
import cn.chatme.message.notification.ModifyGroupExtraNotificationContent;
import cn.chatme.message.notification.ModifyGroupMemberExtraNotificationContent;
import cn.chatme.message.notification.NotificationMessageContent;
import cn.chatme.message.notification.QuitGroupNotificationContent;
import cn.chatme.message.notification.QuitGroupVisibleNotificationContent;
import cn.chatme.message.notification.StartSecretChatMessageContent;
import cn.chatme.message.notification.TipNotificationContent;
import cn.chatme.message.notification.TransferGroupOwnerNotificationContent;

@MessageContentType(value = {
    AddGroupMemberNotificationContent.class,
    ChangeGroupNameNotificationContent.class,
    ChangeGroupPortraitNotificationContent.class,
    CreateGroupNotificationContent.class,
    DismissGroupNotificationContent.class,
    DismissGroupNotificationContent.class,
    KickoffGroupMemberNotificationContent.class,
    ModifyGroupAliasNotificationContent.class,
    ModifyGroupExtraNotificationContent.class,
    ModifyGroupMemberExtraNotificationContent.class,
    QuitGroupNotificationContent.class,
    QuitGroupVisibleNotificationContent.class,
    KickoffGroupMemberVisibleNotificationContent.class,
    TransferGroupOwnerNotificationContent.class,
    TipNotificationContent.class,
    FriendAddedMessageContent.class,
    FriendGreetingMessageContent.class,
    GroupMuteNotificationContent.class,
    GroupPrivateChatNotificationContent.class,
    GroupJoinTypeNotificationContent.class,
    GroupSetManagerNotificationContent.class,
    GroupMuteMemberNotificationContent.class,
    GroupAllowMemberNotificationContent.class,
    AddParticipantsMessageContent.class,
    StartSecretChatMessageContent.class
    // TODO add more

})
/**
 * 小灰条消息, 居中显示，且不显示发送者，用于简单通知，如果需要扩展成复杂通知，可以参考 {@link ExampleRichNotificationMessageContentViewHolder}
 *
 */
public class SimpleNotificationMessageContentViewHolder extends NotificationMessageContentViewHolder {

    TextView notificationTextView;

    public SimpleNotificationMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        notificationTextView =itemView.findViewById(R.id.notificationTextView);
    }

    @Override
    public void onBind(UiMessage message, int position) {
        super.onBind(message, position);
        onBind(message);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        return true;
    }

    protected void onBind(UiMessage message) {
        String notification;
        try {
            notification = ((NotificationMessageContent) message.message.content).formatNotification(message.message);
        } catch (Exception e) {
            e.printStackTrace();
            notification = "message is invalid";
        }
        notificationTextView.setText(notification);
    }

}
