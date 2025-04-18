/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.content.Context;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ConversationContextMenuItem;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;

@ConversationInfoType(type = Conversation.ConversationType.Channel, line = 0)
@EnableContextMenu
public class ChannelConversationViewHolder extends ConversationViewHolder {

    public ChannelConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        ChannelInfo channelInfo = ChatManager.Instance().getChannelInfo(conversationInfo.conversation.target, false);
        String name;
        String portrait;
        if (channelInfo != null) {
            name = channelInfo.name;
            portrait = channelInfo.portrait;
        } else {
            name = "Channel<" + conversationInfo.conversation.target + ">";
            portrait = null;
        }
        nameTextView.setText(name);
        Glide
            .with(fragment)
            .load(portrait)
            .placeholder(R.mipmap.ic_channel)
            .transform(centerCropTransformation, roundedCornerTransformation)
            .into(portraitImageView);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_UNSUBSCRIBE,
        confirm = true,
        priority = 0)
    public void unSubscribeChannel(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.unSubscribeChannel(conversationInfo);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (ConversationContextMenuItemTags.TAG_UNSUBSCRIBE.equals(tag)) {
            return context.getString(R.string.channel_following);
        }
        return super.contextMenuTitle(context, tag);
    }

    @Override
    public String contextConfirmPrompt(Context context, String tag) {
        if (ConversationContextMenuItemTags.TAG_UNSUBSCRIBE.equals(tag)) {
            return context.getString(R.string.channel_unsubscribe_confirm);
        }
        return super.contextConfirmPrompt(context, tag);
    }
}
