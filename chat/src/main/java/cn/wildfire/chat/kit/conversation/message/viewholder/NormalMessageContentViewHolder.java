package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import butterknife.Bind;
import butterknife.OnClick;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;

/**
 * 普通消息
 */
public abstract class NormalMessageContentViewHolder extends MessageContentViewHolder {
    @Bind(R.id.portraitImageView)
    ImageView portraitImageView;
    @Bind(R.id.errorLinearLayout)
    LinearLayout errorLinearLayout;
    @Bind(R.id.nameTextView)
    TextView nameTextView;

    public NormalMessageContentViewHolder(FragmentActivity activity, RecyclerView.Adapter adapter, View itemView) {
        super(activity, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message, int position) {
        super.onBind(message, position);
        this.message = message;
        this.position = position;

        setSenderAvatar(message.message);
        setSenderName(message.message);
        setSendStatus(message.message);
        try {
            onBind(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (message.isFocus) {
            highlightItem(itemView, message);
        }
    }

    protected abstract void onBind(UiMessage message);

    /**
     * when animation finish, do not forget to set {@link UiMessage#isFocus} to {@code true}
     *
     * @param itemView the item view
     * @param message  the message to highlight
     */
    protected void highlightItem(View itemView, UiMessage message) {
        Animation animation = new AlphaAnimation((float) 0.4, (float) 0.2);
        itemView.setBackgroundColor(itemView.getResources().getColor(R.color.colorPrimary));
        animation.setRepeatCount(2);
        animation.setDuration(500);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                itemView.setBackground(null);
                message.isFocus = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        itemView.startAnimation(animation);
    }

    // TODO 也用注解来做？
    public boolean checkable(UiMessage message) {
        return true;
    }

    @Nullable
    @OnClick(R.id.errorLinearLayout)
    public void onRetryClick(View itemView) {
        new MaterialDialog.Builder(context)
                .content("重新发送?")
                .negativeText("取消")
                .positiveText("重发")
                .onPositive((dialog, which) -> conversationViewModel.resendMessage(message.message))
                .build()
                .show();
    }


    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_RECALL, title = "撤回", priority = 10)
    public void recall(View itemView, UiMessage message) {
        conversationViewModel.recallMessage(message.message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_DELETE, title = "删除", confirm = true, confirmPrompt = "确认删除此消息", priority = 11)
    public void removeMessage(View itemView, UiMessage message) {
        conversationViewModel.deleteMessage(message.message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_FORWARD, title = "转发", priority = 11)
    public void forwardMessage(View itemView, UiMessage message) {
        Intent intent = new Intent(context, ForwardActivity.class);
        intent.putExtra("message", message.message);
        context.startActivity(intent);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CHANEL_PRIVATE_CHAT, title = "私聊", priority = 12)
    public void startChanelPrivateChat(View itemView, UiMessage message) {
        Intent intent = ConversationActivity.buildConversationIntent(context, Conversation.ConversationType.Channel, message.message.conversation.target, message.message.conversation.line, message.message.sender);
        context.startActivity(intent);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        Message message = uiMessage.message;
        if (MessageContextMenuItemTags.TAG_RECALL.equals(tag)) {

            String userId = ChatManager.Instance().getUserId();
            if (message.conversation.type == Conversation.ConversationType.Group) {
                GroupViewModel groupViewModel = ViewModelProviders.of(context).get(GroupViewModel.class);
                GroupInfo groupInfo = groupViewModel.getGroupInfo(message.conversation.target, false);
                if (groupInfo != null && userId.equals(groupInfo.owner)) {
                    return false;
                }
                GroupMember groupMember = groupViewModel.getGroupMember(message.conversation.target, ChatManager.Instance().getUserId());
                if (groupMember != null && (groupMember.type == GroupMember.GroupMemberType.Manager
                        || groupMember.type == GroupMember.GroupMemberType.Owner)) {
                    return false;
                } else {
                    return true;
                }
            } else {
                long delta = ChatManager.Instance().getServerDeltaTime();
                long now = System.currentTimeMillis();
                if (message.direction == MessageDirection.Send
                        && TextUtils.equals(message.sender, ChatManager.Instance().getUserId())
                        && now - (message.serverTime - delta) < 60 * 1000) {
                    return false;
                } else {
                    return true;
                }
            }
        }

        if (uiMessage.message.content instanceof NotificationMessageContent && MessageContextMenuItemTags.TAG_FORWARD.equals(tag)) {
            return true;
        }

        // 只有channel 主可以发起
        if (MessageContextMenuItemTags.TAG_CHANEL_PRIVATE_CHAT.equals(tag)) {
            if (uiMessage.message.conversation.type == Conversation.ConversationType.Channel
                    && uiMessage.message.direction == MessageDirection.Receive
                    && conversationViewModel.getChannelPrivateChatUser() == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    private void setSenderAvatar(Message item) {
        // TODO get user info from viewModel
        UserInfo userInfo = ChatManagerHolder.gChatManager.getUserInfo(item.sender, false);
        if (userInfo != null && !TextUtils.isEmpty(userInfo.portrait) && portraitImageView != null) {
            GlideApp
                    .with(context)
                    .load(userInfo.portrait)
                    .transforms(new CenterCrop(), new RoundedCorners(10))
                    .into(portraitImageView);
        }
    }

    private void setSenderName(Message item) {
        if (item.conversation.type == Conversation.ConversationType.Single) {
            nameTextView.setVisibility(View.GONE);
        } else if (item.conversation.type == Conversation.ConversationType.Group) {
            showGroupMemberAlias(message.message.conversation, message.message.sender);
        } else {
            // todo
        }
    }

    private void showGroupMemberAlias(Conversation conversation, String sender) {
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        if (!"1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, conversation.target))) {
            nameTextView.setVisibility(View.GONE);
            return;
        }
        nameTextView.setVisibility(View.VISIBLE);
        // TODO optimize 缓存userInfo吧
//        if (Conversation.equals(nameTextView.getTag(), sender)) {
//            return;
//        }
        GroupViewModel groupViewModel = ViewModelProviders.of(context).get(GroupViewModel.class);

        nameTextView.setText(groupViewModel.getGroupMemberDisplayName(conversation.target, sender));
        nameTextView.setTag(sender);
    }

    protected void setSendStatus(Message item) {
        MessageContent msgContent = item.content;
        MessageStatus sentStatus = item.status;
        if (sentStatus == MessageStatus.Sending) {
            errorLinearLayout.setVisibility(View.GONE);
        } else if (sentStatus == MessageStatus.Send_Failure) {
            errorLinearLayout.setVisibility(View.VISIBLE);
        } else if (sentStatus == MessageStatus.Sent) {
            errorLinearLayout.setVisibility(View.GONE);
        }
    }
}
