package cn.wildfire.chat.conversationlist.viewholder;

import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.lqr.emoji.MoonUtils;
import cn.wildfirechat.chat.R;
import cn.wildfire.chat.ChatManagerHolder;

import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfire.chat.GlideApp;
import cn.wildfire.chat.annotation.ConversationContextMenuItem;
import cn.wildfire.chat.conversation.ConversationActivity;
import cn.wildfire.chat.conversation.Draft;
import cn.wildfire.chat.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.third.utils.TimeUtils;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

@SuppressWarnings("unused")
public class ConversationViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    protected View itemView;
    protected ConversationInfo conversationInfo;
    protected int position;
    protected RecyclerView.Adapter adapter;
    protected ConversationListViewModel conversationListViewModel;

    @Bind(R.id.nameTextView)
    protected TextView nameTextView;
    @Bind(R.id.timeTextView)
    protected TextView timeTextView;
    @Bind(R.id.portraitImageView)
    protected ImageView portraitImageView;
    @Bind(R.id.slient)
    protected ImageView silentImageView;
    @Bind(R.id.unreadCountTextView)
    protected TextView unreadCountTextView;
    @Bind(R.id.contentTextView)
    protected TextView contentTextView;
    @Bind(R.id.promptTextView)
    protected TextView promptTextView;

    @Bind(R.id.statusImageView)
    protected ImageView statusImageView;

    public ConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.itemView = itemView;
        this.adapter = adapter;
        ButterKnife.bind(this, itemView);
        conversationListViewModel = ViewModelProviders
                .of(fragment, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group), Arrays.asList(0)))
                .get(ConversationListViewModel.class);
    }

    final public void onBind(ConversationInfo conversationInfo, int position) {
        this.conversationInfo = conversationInfo;
        this.position = position;
        onBind(conversationInfo);
    }

    public void onBind(ConversationInfo conversationInfo) {
        if (conversationInfo.conversation.type == Conversation.ConversationType.Single) {
            UserInfo userInfo = ChatManagerHolder.gChatManager.getUserInfo(conversationInfo.conversation.target, false);
            if (userInfo != null) {
                GlideApp
                        .with(fragment)
                        .load(userInfo.portrait)
                        .placeholder(R.mipmap.avatar_def)
                        .transforms(new CenterCrop(), new RoundedCorners(10))
                        .into(portraitImageView);
                nameTextView.setText(userInfo.displayName);
                portraitImageView.setVisibility(View.VISIBLE);
            }
        } else {
            GroupInfo groupInfo = ChatManagerHolder.gChatManager.getGroupInfo(conversationInfo.conversation.target, false);
            if (groupInfo != null) {
                ImageView ivHeader = getView(R.id.portraitImageView);
                GlideApp
                        .with(fragment)
                        .load(groupInfo.portrait)
                        .placeholder(R.mipmap.ic_group_cheat)
                        .transforms(new CenterCrop(), new RoundedCorners(10))
                        .into(ivHeader);
                //群昵称
                nameTextView.setText(groupInfo.name);
                setViewVisibility(R.id.portraitImageView, View.VISIBLE);
            }
        }
        timeTextView.setText(TimeUtils.getMsgFormatTime(conversationInfo.timestamp));
        silentImageView.setVisibility(conversationInfo.isSilent ? View.VISIBLE : View.GONE);


        itemView.setBackgroundResource(conversationInfo.isTop ? R.drawable.selector_stick_top_item : R.drawable.selector_common_item);
        if (conversationInfo.unreadCount.unread > 0) {
            unreadCountTextView.setVisibility(View.VISIBLE);
            unreadCountTextView.setText(conversationInfo.unreadCount.unread + "");
        } else {
            unreadCountTextView.setVisibility(View.GONE);
        }

        Draft draft = Draft.fromDraftJson(conversationInfo.draft);
        if (draft != null && !TextUtils.isEmpty(draft.getContent())) {
            MoonUtils.identifyFaceExpression(fragment.getActivity(), contentTextView, draft.getContent(), ImageSpan.ALIGN_BOTTOM);
            setViewVisibility(R.id.promptTextView, View.VISIBLE);
            setViewVisibility(R.id.contentTextView, View.VISIBLE);
        } else {
            if (conversationInfo.unreadCount.unreadMentionAll > 0 || conversationInfo.unreadCount.unreadMention > 0) {
                promptTextView.setText("[有人@我]");
            } else {
                setViewVisibility(R.id.promptTextView, View.GONE);
            }
            setViewVisibility(R.id.contentTextView, View.VISIBLE);
            if (conversationInfo.lastMessage != null && conversationInfo.lastMessage.content != null) {
                String content;
                Message lastMessage = conversationInfo.lastMessage;
                if (conversationInfo.conversation.type == Conversation.ConversationType.Group && lastMessage.direction == MessageDirection.Receive) {
                    UserInfo userInfo = ChatManager.Instance().getUserInfo(conversationInfo.lastMessage.sender, false);
                    content = (userInfo == null ? "<" + conversationInfo.lastMessage.sender + ">" : userInfo.displayName) + ":" + lastMessage.content.digest();
                } else {
                    content = lastMessage.content.digest();
                }
                MoonUtils.identifyFaceExpression(fragment.getActivity(), contentTextView, content, ImageSpan.ALIGN_BOTTOM);

                switch (lastMessage.status) {
                    case Sending:
                        statusImageView.setVisibility(View.VISIBLE);
                        // TODO update sending image resource
                        statusImageView.setImageResource(R.mipmap.ic_add);
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
        conversationListViewModel.clearConversationUnreadStatus(conversationInfo);
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
        conversationListViewModel.removeConversation(conversationInfo);
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
