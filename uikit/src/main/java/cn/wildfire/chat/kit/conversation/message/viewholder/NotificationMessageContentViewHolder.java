/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;

public abstract class NotificationMessageContentViewHolder extends MessageContentViewHolder {
    public NotificationMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        return true;
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return null;
    }

    @Override
    public String contextConfirmPrompt(Context context, String tag) {
        return null;
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
}
