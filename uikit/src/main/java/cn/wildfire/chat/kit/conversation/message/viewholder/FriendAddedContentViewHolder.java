/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.notification.FriendAddedMessageContent;


@MessageContentType(value = {
    FriendAddedMessageContent.class,
})
@EnableContextMenu
public class FriendAddedContentViewHolder extends NormalMessageContentViewHolder {
    TextView contentTextView;

    public FriendAddedContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.contentTextView).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        contentTextView = itemView.findViewById(R.id.contentTextView);
    }

    @Override
    public void onBind(UiMessage message) {
        FriendAddedMessageContent content = (FriendAddedMessageContent) message.message.content;
        contentTextView.setText(content.formatNotification(message.message));
    }

    public void onClick(View view) {
        String content = ((FriendAddedMessageContent) message.message.content).formatNotification(message.message);
        WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), fragment.getString(R.string.message_detail_title), content);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CLIP, confirm = false, priority = 12)
    public void clip(View itemView, UiMessage message) {
        ClipboardManager clipboardManager = (ClipboardManager) fragment.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        FriendAddedMessageContent content = (FriendAddedMessageContent) message.message.content;
        ClipData clipData = ClipData.newPlainText("messageContent", content.formatNotification(message.message));
        clipboardManager.setPrimaryClip(clipData);
    }


    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_CLIP.equals(tag)) {
            return context.getString(R.string.message_copy);
        }
        return super.contextMenuTitle(context, tag);
    }
}
