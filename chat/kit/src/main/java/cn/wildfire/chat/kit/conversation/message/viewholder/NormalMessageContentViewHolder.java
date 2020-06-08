package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.core.PersistFlag;
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
    @BindView(R.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R.id.errorLinearLayout)
    LinearLayout errorLinearLayout;
    @BindView(R.id.nameTextView)
    TextView nameTextView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.checkbox)
    CheckBox checkBox;

    @BindView(R.id.singleReceiptImageView)
    @Nullable
    ImageView singleReceiptImageView;

    @BindView(R.id.groupReceiptFrameLayout)
    @Nullable
    FrameLayout groupReceiptFrameLayout;

    @BindView(R.id.deliveryProgressBar)
    @Nullable
    ProgressBar deliveryProgressBar;
    @BindView(R.id.readProgressBar)
    @Nullable
    ProgressBar readProgressBar;

    public NormalMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
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

    @Optional
    @OnClick(R.id.errorLinearLayout)
    public void onRetryClick(View itemView) {
        new MaterialDialog.Builder(fragment.getContext())
            .content("重新发送?")
            .negativeText("取消")
            .positiveText("重发")
            .onPositive((dialog, which) -> messageViewModel.resendMessage(message.message))
            .build()
            .show();
    }

    @Optional
    @OnClick(R.id.groupReceiptFrameLayout)
    public void OnGroupMessageReceiptClick(View itemView) {
        ((ConversationMessageAdapter) adapter).onGroupMessageReceiptClick(message.message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_RECALL, title = "撤回", priority = 10)
    public void recall(View itemView, UiMessage message) {
        messageViewModel.recallMessage(message.message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_DELETE, title = "删除", confirm = true, confirmPrompt = "确认删除此消息", priority = 11)
    public void removeMessage(View itemView, UiMessage message) {
        messageViewModel.deleteMessage(message.message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_FORWARD, title = "转发", priority = 11)
    public void forwardMessage(View itemView, UiMessage message) {
        Intent intent = new Intent(fragment.getContext(), ForwardActivity.class);
        intent.putExtra("message", message.message);
        fragment.startActivity(intent);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_MULTI_CHECK, title = "多选", priority = 13)
    public void checkMessage(View itemView, UiMessage message) {
        fragment.toggleMultiMessageMode(message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CHANEL_PRIVATE_CHAT, title = "私聊", priority = 12)
    public void startChanelPrivateChat(View itemView, UiMessage message) {
        Intent intent = ConversationActivity.buildConversationIntent(fragment.getContext(), Conversation.ConversationType.Channel, message.message.conversation.target, message.message.conversation.line, message.message.sender);
        fragment.startActivity(intent);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        Message message = uiMessage.message;
        if (MessageContextMenuItemTags.TAG_RECALL.equals(tag)) {
            String userId = ChatManager.Instance().getUserId();
            if (message.conversation.type == Conversation.ConversationType.Group) {
                GroupViewModel groupViewModel = ViewModelProviders.of(fragment).get(GroupViewModel.class);
                GroupInfo groupInfo = groupViewModel.getGroupInfo(message.conversation.target, false);
                if (groupInfo != null && userId.equals(groupInfo.owner)) {
                    return false;
                }
                GroupMember groupMember = groupViewModel.getGroupMember(message.conversation.target, ChatManager.Instance().getUserId());
                if (groupMember != null && (groupMember.type == GroupMember.GroupMemberType.Manager
                    || groupMember.type == GroupMember.GroupMemberType.Owner)) {
                    return false;
                }
            }

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

        // 只有channel 主可以发起
        if (MessageContextMenuItemTags.TAG_CHANEL_PRIVATE_CHAT.equals(tag)) {
            if (uiMessage.message.conversation.type == Conversation.ConversationType.Channel
                && uiMessage.message.direction == MessageDirection.Receive) {
                return false;
            }
            return true;
        }
        return false;
    }

    private void setSenderAvatar(Message item) {
        // TODO get user info from viewModel
        UserInfo userInfo = ChatManagerHolder.gChatManager.getUserInfo(item.sender, false);
        if (portraitImageView != null) {
            GlideApp
                .with(fragment)
                .load(userInfo.portrait)
                .transforms(new CenterCrop(), new RoundedCorners(10))
                .placeholder(R.mipmap.avatar_def)
                .into(portraitImageView);
        }
    }

    private void setSenderName(Message item) {
        if (item.conversation.type == Conversation.ConversationType.Single) {
            nameTextView.setVisibility(View.GONE);
        } else if (item.conversation.type == Conversation.ConversationType.Group) {
            showGroupMemberAlias(message.message.conversation, message.message, message.message.sender);
        } else {
            // todo
        }
    }

    private void showGroupMemberAlias(Conversation conversation, Message message, String sender) {
        UserViewModel userViewModel = ViewModelProviders.of(fragment).get(UserViewModel.class);
        if (!"1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, conversation.target)) || message.direction == MessageDirection.Send) {
            nameTextView.setVisibility(View.GONE);
            return;
        }
        nameTextView.setVisibility(View.VISIBLE);
        // TODO optimize 缓存userInfo吧
//        if (Conversation.equals(nameTextView.getTag(), sender)) {
//            return;
//        }
        GroupViewModel groupViewModel = ViewModelProviders.of(fragment).get(GroupViewModel.class);

        nameTextView.setText(groupViewModel.getGroupMemberDisplayName(conversation.target, sender));
        nameTextView.setTag(sender);
    }

    protected boolean showMessageReceipt(Message message) {
        ContentTag tag = message.content.getClass().getAnnotation(ContentTag.class);
        return (tag != null && (tag.flag() == PersistFlag.Persist_And_Count));
    }

    protected void setSendStatus(Message item) {
        MessageStatus sentStatus = item.status;
        if (item.direction == MessageDirection.Receive) {
            return;
        }
        if (sentStatus == MessageStatus.Sending) {
            progressBar.setVisibility(View.VISIBLE);
            errorLinearLayout.setVisibility(View.GONE);
            return;
        } else if (sentStatus == MessageStatus.Send_Failure) {
            progressBar.setVisibility(View.GONE);
            errorLinearLayout.setVisibility(View.VISIBLE);
            return;
        } else if (sentStatus == MessageStatus.Sent) {
            progressBar.setVisibility(View.GONE);
            errorLinearLayout.setVisibility(View.GONE);
        } else if (sentStatus == MessageStatus.Readed) {
            progressBar.setVisibility(View.GONE);
            errorLinearLayout.setVisibility(View.GONE);
            return;
        }

        if (!ChatManager.Instance().isReceiptEnabled() || !ChatManager.Instance().isUserEnableReceipt() || !showMessageReceipt(message.message)) {
            return;
        }

        Map<String, Long> deliveries = ((ConversationMessageAdapter) adapter).getDeliveries();
        Map<String, Long> readEntries = ((ConversationMessageAdapter) adapter).getReadEntries();

        if (item.conversation.type == Conversation.ConversationType.Single) {
            singleReceiptImageView.setVisibility(View.VISIBLE);
            groupReceiptFrameLayout.setVisibility(View.GONE);
            Long readTimestamp = readEntries != null && !readEntries.isEmpty() ? readEntries.get(message.message.conversation.target) : null;
            Long deliverTimestamp = deliveries != null && !deliveries.isEmpty() ? deliveries.get(message.message.conversation.target) : null;


            if (readTimestamp != null && readTimestamp >= message.message.serverTime) {
                ImageViewCompat.setImageTintList(singleReceiptImageView, null);
                return;
            }
            if (deliverTimestamp != null && deliverTimestamp >= message.message.serverTime) {
                ImageViewCompat.setImageTintList(singleReceiptImageView, ColorStateList.valueOf(ContextCompat.getColor(fragment.getContext(), R.color.gray)));
            }
        } else if (item.conversation.type == Conversation.ConversationType.Group) {
            singleReceiptImageView.setVisibility(View.GONE);
            groupReceiptFrameLayout.setVisibility(View.VISIBLE);

            if (sentStatus == MessageStatus.Sent) {
                int deliveryCount = 0;
                if (deliveries != null) {
                    for (Map.Entry<String, Long> delivery : deliveries.entrySet()) {
                        if (delivery.getValue() >= item.serverTime) {
                            deliveryCount++;
                        }
                    }
                }
                int readCount = 0;
                if (readEntries != null) {
                    for (Map.Entry<String, Long> readEntry : readEntries.entrySet()) {
                        if (readEntry.getValue() >= item.serverTime) {
                            readCount++;
                        }
                    }
                }

                GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(item.conversation.target, false);
                if (groupInfo == null) {
                    return;
                }
                deliveryProgressBar.setMax(groupInfo.memberCount - 1);
                deliveryProgressBar.setProgress(deliveryCount);
                readProgressBar.setMax(groupInfo.memberCount - 1);
                readProgressBar.setProgress(readCount);
            } else {
                groupReceiptFrameLayout.setVisibility(View.GONE);
            }
        }
    }
}
