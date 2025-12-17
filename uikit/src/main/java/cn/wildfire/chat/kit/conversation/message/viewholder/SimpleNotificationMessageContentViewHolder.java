/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.AddParticipantsMessageContent;
import cn.wildfirechat.message.NotDeliveredMessageContent;
import cn.wildfirechat.message.notification.AddGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.ChangeGroupNameNotificationContent;
import cn.wildfirechat.message.notification.ChangeGroupPortraitNotificationContent;
import cn.wildfirechat.message.notification.CreateGroupNotificationContent;
import cn.wildfirechat.message.notification.DismissGroupNotificationContent;
import cn.wildfirechat.message.notification.FriendGreetingMessageContent;
import cn.wildfirechat.message.notification.GroupAllowMemberNotificationContent;
import cn.wildfirechat.message.notification.GroupJoinTypeNotificationContent;
import cn.wildfirechat.message.notification.GroupMuteMemberNotificationContent;
import cn.wildfirechat.message.notification.GroupMuteNotificationContent;
import cn.wildfirechat.message.notification.GroupPrivateChatNotificationContent;
import cn.wildfirechat.message.notification.GroupRejectJoinNotificationContent;
import cn.wildfirechat.message.notification.GroupSetManagerNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberVisibleNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupAliasNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupExtraNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupMemberExtraNotificationContent;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.message.notification.QuitGroupNotificationContent;
import cn.wildfirechat.message.notification.QuitGroupVisibleNotificationContent;
import cn.wildfirechat.message.notification.StartSecretChatMessageContent;
import cn.wildfirechat.message.notification.TipNotificationContent;
import cn.wildfirechat.message.notification.TransferGroupOwnerNotificationContent;

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
    FriendGreetingMessageContent.class,
    GroupMuteNotificationContent.class,
    GroupPrivateChatNotificationContent.class,
    GroupRejectJoinNotificationContent.class,
    GroupJoinTypeNotificationContent.class,
    GroupSetManagerNotificationContent.class,
    GroupMuteMemberNotificationContent.class,
    GroupAllowMemberNotificationContent.class,
    AddParticipantsMessageContent.class,
    StartSecretChatMessageContent.class,
    NotDeliveredMessageContent.class
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
            notification = fragment.getString(R.string.message_invalid);
        }
        notificationTextView.setText(notification);
    }

}
