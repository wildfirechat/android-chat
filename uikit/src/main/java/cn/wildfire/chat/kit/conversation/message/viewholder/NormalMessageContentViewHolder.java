/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.message.ArticlesMessageContent;
import cn.wildfirechat.message.CallStartMessageContent;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.StickerMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 普通消息
 */
public abstract class NormalMessageContentViewHolder extends MessageContentViewHolder {
    ImageView portraitImageView;
    LinearLayout errorLinearLayout;
    TextView nameTextView;
    ProgressBar progressBar;
    CheckBox checkBox;

    ImageView singleReceiptImageView;
    FrameLayout groupReceiptFrameLayout;
    ProgressBar deliveryProgressBar;
    ProgressBar readProgressBar;
    UserInfo senderUserInfo;

    public NormalMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        if (errorLinearLayout != null) {
            errorLinearLayout.setOnClickListener(this::onRetryClick);
        }
        if (groupReceiptFrameLayout != null) {
            groupReceiptFrameLayout.setOnClickListener(this::OnGroupMessageReceiptClick);
        }
    }

    private void bindViews(View itemView) {
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        errorLinearLayout = itemView.findViewById(R.id.errorLinearLayout);
        nameTextView = itemView.findViewById(R.id.nameTextView);
        progressBar = itemView.findViewById(R.id.progressBar);
        checkBox = itemView.findViewById(R.id.checkbox);
        singleReceiptImageView = itemView.findViewById(R.id.singleReceiptImageView);
        groupReceiptFrameLayout = itemView.findViewById(R.id.groupReceiptFrameLayout);
        deliveryProgressBar = itemView.findViewById(R.id.deliveryProgressBar);
        readProgressBar = itemView.findViewById(R.id.readProgressBar);
    }

    @Override
    public void onBind(UiMessage message, int position) {
        super.onBind(message, position);

        String tag = (String) itemView.getTag();
        if (!TextUtils.equals(tag, message.message.sender)) {
            setSenderAvatar(message.message);
        }
        setSenderName(message.message);
        setMessageStatus(message.message);
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

    public void onRetryClick(View itemView) {
        new MaterialDialog.Builder(fragment.getContext())
            .content(R.string.message_resend_title)
            .negativeText(R.string.forward_cancel)
            .positiveText(R.string.message_resend)
            .onPositive((dialog, which) -> messageViewModel.resendMessage(message.message))
            .build()
            .show();
    }

    public void OnGroupMessageReceiptClick(View itemView) {
        ((ConversationMessageAdapter) adapter).onGroupMessageReceiptClick(message.message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_RECALL, priority = 10)
    public void recall(View itemView, UiMessage message) {
        messageViewModel.recallMessage(message.message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_DELETE, confirm = false, priority = 11)
    public void removeMessage(View itemView, UiMessage message) {

        List<String> items = new ArrayList<>();
        items.add(itemView.getContext().getString(R.string.message_delete_local));
        boolean isSuperGroup = false;
        if (message.message.conversation.type == Conversation.ConversationType.Group) {
            GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(message.message.conversation.target, false);
            if (groupInfo != null && groupInfo.superGroup == 1) {
                isSuperGroup = true;
            }
        }
        // 超级群组不支持远端删除
        if ((message.message.conversation.type == Conversation.ConversationType.Group && !isSuperGroup)
            || message.message.conversation.type == Conversation.ConversationType.Single
            || message.message.conversation.type == Conversation.ConversationType.Channel
        ) {
            items.add(itemView.getContext().getString(R.string.message_delete_remote));
        } else if (message.message.conversation.type == Conversation.ConversationType.SecretChat) {
            items.add(itemView.getContext().getString(R.string.message_delete_both));
        }

        new MaterialDialog.Builder(fragment.getContext())
            .items(items)
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                    if (position == 0) {
                        messageViewModel.deleteMessage(message.message);
                    } else {
                        messageViewModel.deleteRemoteMessage(message.message);
                    }
                }
            })
            .show();
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_FORWARD, priority = 11)
    public void forwardMessage(View itemView, UiMessage message) {
        Intent intent = new Intent(fragment.getContext(), ForwardActivity.class);
        intent.putExtra("message", message.message);
        fragment.startActivity(intent);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_MULTI_CHECK, priority = 13)
    public void checkMessage(View itemView, UiMessage message) {
        fragment.toggleMultiMessageMode(message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CHANNEL_PRIVATE_CHAT, priority = 12)
    public void startChanelPrivateChat(View itemView, UiMessage message) {
        Intent intent = ConversationActivity.buildConversationIntent(fragment.getContext(), Conversation.ConversationType.Channel, message.message.conversation.target, message.message.conversation.line, message.message.sender);
        fragment.startActivity(intent);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_QUOTE, priority = 14)
    public void quoteMessage(View itemView, UiMessage message) {
        fragment.getConversationInputPanel().quoteMessage(message.message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_FAV, confirm = false, priority = 12)
    public void fav(View itemView, UiMessage message) {
        AppServiceProvider appServiceProvider = WfcUIKit.getWfcUIKit().getAppServiceProvider();
        FavoriteItem favoriteItem = FavoriteItem.fromMessage(message.message);

        appServiceProvider.addFavoriteItem(favoriteItem, new SimpleCallback<Void>() {
            @Override
            public void onUiSuccess(Void aVoid) {
                Toast.makeText(fragment.getContext(), "收藏成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(fragment.getContext(), "收藏失败: " + code, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        String title = context.getString(R.string.message_unknown_option);
        switch (tag) {
            case MessageContextMenuItemTags.TAG_RECALL:
                title = context.getString(R.string.message_recall);
                break;
            case MessageContextMenuItemTags.TAG_DELETE:
                title = context.getString(R.string.message_delete);
                break;
            case MessageContextMenuItemTags.TAG_FORWARD:
                title = context.getString(R.string.message_forward);
                break;
            case MessageContextMenuItemTags.TAG_QUOTE:
                title = context.getString(R.string.message_quote);
                break;
            case MessageContextMenuItemTags.TAG_MULTI_CHECK:
                title = context.getString(R.string.message_multi_select);
                break;
            case MessageContextMenuItemTags.TAG_CHANNEL_PRIVATE_CHAT:
                title = context.getString(R.string.message_private_chat);
                break;
            case MessageContextMenuItemTags.TAG_FAV:
                title = context.getString(R.string.message_favorite);
                break;
            default:
                break;
        }
        return title;
    }

    @Override
    public int contextMenuIcon(Context context, String tag) {
        int resId = 0;
        switch (tag) {
            case MessageContextMenuItemTags.TAG_RECALL:
                resId = R.mipmap.ic_msg_rollback;
                break;
            case MessageContextMenuItemTags.TAG_DELETE:
                resId = R.mipmap.ic_msg_delete;
                break;
            case MessageContextMenuItemTags.TAG_FORWARD:
                resId = R.mipmap.ic_msg_forward;
                break;
            case MessageContextMenuItemTags.TAG_QUOTE:
                resId = R.mipmap.ic_msg_quote;
                break;
            case MessageContextMenuItemTags.TAG_MULTI_CHECK:
                resId = R.mipmap.ic_msg_select;
                break;
            case MessageContextMenuItemTags.TAG_CHANNEL_PRIVATE_CHAT:
                resId = R.mipmap.ic_msg_select;
                break;
            case MessageContextMenuItemTags.TAG_FAV:
                resId = R.mipmap.ic_msg_collect;
                break;
            case MessageContextMenuItemTags.TAG_CLIP:
                resId = R.mipmap.ic_msg_copy;
                break;
            case MessageContextMenuItemTags.TAG_SPEECH_TO_TEXT:
                resId = R.mipmap.ic_msg_quote;
                break;
            default:
                break;
        }
        return resId;
    }

    @Override
    public String contextConfirmPrompt(Context context, String tag) {
        String title = context.getString(R.string.message_unknown_option);
        switch (tag) {
            case MessageContextMenuItemTags.TAG_DELETE:
                title = context.getString(R.string.message_delete_confirm);
                break;
            default:
                break;
        }
        return title;
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        Message message = uiMessage.message;

        if (MessageContextMenuItemTags.TAG_RECALL.equals(tag)) {
            MessageContent messageContent = message.content;
            if (messageContent instanceof CallStartMessageContent) {
                return true;
            }
            if (message.conversation.type == Conversation.ConversationType.Group) {
                GroupViewModel groupViewModel = new ViewModelProvider(fragment).get(GroupViewModel.class);
                GroupMember groupMember = groupViewModel.getGroupMember(message.conversation.target, ChatManager.Instance().getUserId());
                GroupMember fromMember = groupViewModel.getGroupMember(message.conversation.target, message.sender);
                if (groupMember == null || fromMember == null) {
                    return true;
                }
                // 群主允许撤回所有消息
                if (groupMember.type == GroupMember.GroupMemberType.Owner) {
                    return false;
                }

                // 管理员可以测试普通成员的消息
                if (groupMember.type == GroupMember.GroupMemberType.Manager
                    && (fromMember.type != GroupMember.GroupMemberType.Owner && fromMember.type != GroupMember.GroupMemberType.Manager)) {
                    return false;
                }
            }

            long delta = ChatManager.Instance().getServerDeltaTime();
            long now = System.currentTimeMillis();
            if (message.direction == MessageDirection.Send
                && TextUtils.equals(message.sender, ChatManager.Instance().getUserId())
                && now - (message.serverTime - delta) < Config.RECALL_TIME_LIMIT * 1000) {
                return false;
            } else {
                return true;
            }
        }

        // 只有channel 主可以发起
        if (MessageContextMenuItemTags.TAG_CHANNEL_PRIVATE_CHAT.equals(tag)) {
            if (uiMessage.message.conversation.type == Conversation.ConversationType.Channel
                && uiMessage.message.direction == MessageDirection.Receive) {
                return false;
            }
            return true;
        }

        // 只有部分消息支持引用
        if (MessageContextMenuItemTags.TAG_QUOTE.equals(tag)) {
            MessageContent messageContent = message.content;
            if (messageContent instanceof TextMessageContent
                || messageContent instanceof FileMessageContent
                || messageContent instanceof VideoMessageContent
                || messageContent instanceof StickerMessageContent
                || messageContent instanceof ImageMessageContent) {
                return false;
            }
            return true;
        }

        // 只有部分消息支持收藏
        if (MessageContextMenuItemTags.TAG_FAV.equals(tag)) {
            if (message.conversation.type == Conversation.ConversationType.SecretChat) {
                return true;
            }
            MessageContent messageContent = message.content;
            if (messageContent instanceof TextMessageContent
                || messageContent instanceof FileMessageContent
                || messageContent instanceof CompositeMessageContent
                || messageContent instanceof VideoMessageContent
                || messageContent instanceof SoundMessageContent
                || messageContent instanceof ArticlesMessageContent
                || messageContent instanceof ImageMessageContent) {
                return false;
            }
            return true;
        }

        if (MessageContextMenuItemTags.TAG_FORWARD.equals(tag)) {
            if (message.conversation.type == Conversation.ConversationType.SecretChat) {
                return true;
            }
            MessageContent messageContent = message.content;
            if (messageContent instanceof SoundMessageContent
                || messageContent instanceof CallStartMessageContent) {
                return true;
            }
            return false;
        }

        if (MessageContextMenuItemTags.TAG_MULTI_CHECK.equals(tag)) {
            MessageContent messageContent = message.content;
            if (messageContent instanceof SoundMessageContent
                || messageContent instanceof CallStartMessageContent) {
                return true;
            }
            return false;
        }

        return false;
    }

    private void setSenderAvatar(Message item) {
        // TODO get user info from viewModel
        String portraitUrl = null;
        if (item.conversation.type == Conversation.ConversationType.Channel && item.direction == MessageDirection.Receive) {
            senderUserInfo = ChatManager.Instance().getUserInfo(item.sender, false);
            if (senderUserInfo != null) {
                portraitUrl = senderUserInfo.portrait;
            }
        } else {
            senderUserInfo = ChatManager.Instance().getUserInfo(item.sender, item.conversation.type == Conversation.ConversationType.Group ? item.conversation.target : null, false);
            if (senderUserInfo != null) {
                portraitUrl = senderUserInfo.portrait;
            }
        }
        if (senderUserInfo != null && senderUserInfo.updateDt > 0) {
            itemView.setTag(message.message.sender);
        }
        if (portraitImageView != null && portraitUrl != null) {
            Glide
                .with(fragment)
                .load(portraitUrl)
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
        if (!fragment.isShowGroupMemberName() || message.direction == MessageDirection.Send) {
            nameTextView.setVisibility(View.GONE);
            return;
        }
        nameTextView.setVisibility(View.VISIBLE);
        // TODO optimize 缓存userInfo吧
//        if (Conversation.equals(nameTextView.getTag(), sender)) {
//            return;
//        }
        GroupViewModel groupViewModel = new ViewModelProvider(fragment).get(GroupViewModel.class);

        nameTextView.setText(groupViewModel.getGroupMemberDisplayNameEx(senderUserInfo, 11));
        nameTextView.setTag(sender);
    }

    protected boolean showMessageReceipt(Message message) {
        ContentTag tag = message.content.getClass().getAnnotation(ContentTag.class);
        return (tag != null && (tag.flag() == PersistFlag.Persist_And_Count));
    }

    protected void setMessageStatus(Message item) {
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

        if (!ChatManager.Instance().isReceiptEnabled()
            || !ChatManager.Instance().isUserEnableReceipt()
            || (item.conversation.type == Conversation.ConversationType.Group && !ChatManager.Instance().isGroupReceiptEnabled())
            || !showMessageReceipt(message.message)) {

            singleReceiptImageView.setVisibility(View.GONE);
            groupReceiptFrameLayout.setVisibility(View.GONE);
            return;
        }

        Map<String, Long> readEntries = ((ConversationMessageAdapter) adapter).getReadEntries();

        if (item.conversation.type == Conversation.ConversationType.Single) {
            singleReceiptImageView.setVisibility(View.VISIBLE);
            groupReceiptFrameLayout.setVisibility(View.GONE);
            Long readTimestamp = readEntries != null && !readEntries.isEmpty() ? readEntries.get(message.message.conversation.target) : null;

            if (readTimestamp != null && readTimestamp >= message.message.serverTime) {
                ImageViewCompat.setImageTintList(singleReceiptImageView, null);
            } else {
                singleReceiptImageView.setImageResource(R.mipmap.receipt);
            }
        } else if (item.conversation.type == Conversation.ConversationType.Group) {
            singleReceiptImageView.setVisibility(View.GONE);

            if (sentStatus == MessageStatus.Sent) {
                if (item.content instanceof CallStartMessageContent || (item.content.getPersistFlag().ordinal() & 0x2) == 0) {
                    groupReceiptFrameLayout.setVisibility(View.GONE);
                } else {
                    groupReceiptFrameLayout.setVisibility(View.VISIBLE);
                }
//                int deliveryCount = 0;
//                if (deliveries != null) {
//                    for (Map.Entry<String, Long> delivery : deliveries.entrySet()) {
//                        if (delivery.getValue() >= item.serverTime) {
//                            deliveryCount++;
//                        }
//                    }
//                }
                int readCount = 0;
                if (readEntries != null) {
                    for (Map.Entry<String, Long> readEntry : readEntries.entrySet()) {
                        if (readEntry.getValue() >= item.serverTime) {
                            readCount++;
                        }
                    }
                }

                GroupInfo groupInfo;
                groupInfo = (GroupInfo) itemView.getTag(R.id.readProgressBar);
                if (groupInfo == null) {
                    groupInfo = ChatManager.Instance().getGroupInfo(item.conversation.target, false);
                    if (groupInfo != null && groupInfo.updateDt > 0) {
                        itemView.setTag(R.id.readProgressBar, groupInfo);
                    }
                }
                if (groupInfo == null) {
                    return;
                }
//                deliveryProgressBar.setMax(groupInfo.memberCount - 1);
//                deliveryProgressBar.setProgress(deliveryCount);
                readProgressBar.setMax(groupInfo.memberCount - 1);
                readProgressBar.setProgress(readCount);
            } else {
                groupReceiptFrameLayout.setVisibility(View.GONE);
            }
        }
    }
}
