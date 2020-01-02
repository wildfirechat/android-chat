package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.content.Intent;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.lqr.emoji.MoonUtils;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.annotation.ConversationContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.Draft;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;

@SuppressWarnings("unused")
public abstract class ConversationViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    protected View itemView;
    protected ConversationInfo conversationInfo;
    protected RecyclerView.Adapter adapter;
    protected ConversationListViewModel conversationListViewModel;

    @BindView(R.id.nameTextView)
    protected TextView nameTextView;
    @BindView(R.id.timeTextView)
    protected TextView timeTextView;
    @BindView(R.id.portraitImageView)
    protected ImageView portraitImageView;
    @BindView(R.id.slient)
    protected ImageView silentImageView;
    @BindView(R.id.unreadCountTextView)
    protected TextView unreadCountTextView;
    @BindView(R.id.redDotView)
    protected View redDotView;
    @BindView(R.id.contentTextView)
    protected TextView contentTextView;
    @BindView(R.id.promptTextView)
    protected TextView promptTextView;

    @BindView(R.id.statusImageView)
    protected ImageView statusImageView;

    public ConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.itemView = itemView;
        this.adapter = adapter;
        ButterKnife.bind(this, itemView);
        conversationListViewModel = ViewModelProviders
                .of(fragment.getActivity(), new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group), Arrays.asList(0)))
                .get(ConversationListViewModel.class);
    }

    final public void onBind(ConversationInfo conversationInfo, int position) {
        this.conversationInfo = conversationInfo;
        onBind(conversationInfo);
    }

    /**
     * 设置头像、名称
     *
     * @param conversationInfo
     */
    protected abstract void onBindConversationInfo(ConversationInfo conversationInfo);

    public void onBind(ConversationInfo conversationInfo) {
        onBindConversationInfo(conversationInfo);

        timeTextView.setText(TimeUtils.getMsgFormatTime(conversationInfo.timestamp));
        silentImageView.setVisibility(conversationInfo.isSilent ? View.VISIBLE : View.GONE);
        statusImageView.setVisibility(View.GONE);

        itemView.setBackgroundResource(conversationInfo.isTop ? R.drawable.selector_stick_top_item : R.drawable.selector_common_item);
        redDotView.setVisibility(View.GONE);
        if (conversationInfo.isSilent) {
            if (conversationInfo.unreadCount.unread > 0) { // 显示红点
                unreadCountTextView.setText("");
                unreadCountTextView.setVisibility(View.GONE);
                redDotView.setVisibility(View.VISIBLE);
            } else {
                unreadCountTextView.setVisibility(View.GONE);
            }
        } else {
            if (conversationInfo.unreadCount.unread > 0) {
                unreadCountTextView.setVisibility(View.VISIBLE);
                unreadCountTextView.setText(conversationInfo.unreadCount.unread + "");
            } else {
                unreadCountTextView.setVisibility(View.GONE);
            }
        }


        Draft draft = Draft.fromDraftJson(conversationInfo.draft);
        if (draft != null && !TextUtils.isEmpty(draft.getContent())) {
            MoonUtils.identifyFaceExpression(fragment.getActivity(), contentTextView, draft.getContent(), ImageSpan.ALIGN_BOTTOM);
            setViewVisibility(R.id.promptTextView, View.VISIBLE);
            setViewVisibility(R.id.contentTextView, View.VISIBLE);
        } else {
            if (conversationInfo.unreadCount.unreadMentionAll > 0 || conversationInfo.unreadCount.unreadMention > 0) {
                promptTextView.setText("[有人@我]");
                promptTextView.setVisibility(View.VISIBLE);
            } else {
                promptTextView.setVisibility(View.GONE);
            }
            setViewVisibility(R.id.contentTextView, View.VISIBLE);
            if (conversationInfo.lastMessage != null && conversationInfo.lastMessage.content != null) {
                String content = "";
                Message lastMessage = conversationInfo.lastMessage;
                // the message maybe invalid
                try {
                    if (conversationInfo.conversation.type == Conversation.ConversationType.Group
                            && lastMessage.direction == MessageDirection.Receive
                            && !(lastMessage.content instanceof NotificationMessageContent)) {
                        GroupViewModel groupViewModel = ViewModelProviders.of(fragment).get(GroupViewModel.class);
                        String senderDisplayName = groupViewModel.getGroupMemberDisplayName(conversationInfo.conversation.target, conversationInfo.lastMessage.sender);
                        content = senderDisplayName + ":" + lastMessage.digest();
                    } else {
                        content = lastMessage.digest();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                MoonUtils.identifyFaceExpression(fragment.getActivity(), contentTextView, content, ImageSpan.ALIGN_BOTTOM);

                switch (lastMessage.status) {
                    case Sending:
                        statusImageView.setVisibility(View.VISIBLE);
                        // TODO update sending image resource
                        statusImageView.setImageResource(R.mipmap.ic_sending);
                        break;
                    case Send_Failure:
                        statusImageView.setVisibility(View.VISIBLE);
                        statusImageView.setImageResource(R.mipmap.img_error);
                        break;
                    default:
                        statusImageView.setVisibility(View.GONE);
                        break;
                }

            } else {
                contentTextView.setText("");
            }
        }
    }

    public void onClick(View itemView) {
        Intent intent = new Intent(fragment.getActivity(), ConversationActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        fragment.startActivity(intent);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_REMOVE,
            title = "删除会话",
            confirm = true,
            confirmPrompt = "确认删除会话？",
            priority = 0)
    public void removeConversation(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.removeConversation(conversationInfo, true);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_REMOVE,
            title = "清空会话",
            confirm = true,
            confirmPrompt = "确认清空会话？",
            priority = 0)
    public void clearMessages(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.clearMessages(conversationInfo.conversation);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_TOP, title = "置顶", priority = 1)
    public void stickConversationTop(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.setConversationTop(conversationInfo, true);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_CANCEL_TOP, title = "取消置顶", priority = 2)
    public void cancelStickConversationTop(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.setConversationTop(conversationInfo, false);
    }

    /**
     * @param conversationInfo
     * @param itemTag
     * @return 返回true，将从context menu中排除
     */
    public boolean contextMenuItemFilter(ConversationInfo conversationInfo, String itemTag) {
        if (ConversationContextMenuItemTags.TAG_TOP.equals(itemTag)) {
            return conversationInfo.isTop;
        }

        if (ConversationContextMenuItemTags.TAG_CANCEL_TOP.equals(itemTag)) {
            return !conversationInfo.isTop;
        }
        return false;
    }

    protected <T extends View> T getView(int viewId) {
        View view;
        view = itemView.findViewById(viewId);
        return (T) view;
    }

    protected ConversationViewHolder setViewVisibility(int viewId, int visibility) {
        View view = itemView.findViewById(viewId);
        view.setVisibility(visibility);
        return this;
    }

}
