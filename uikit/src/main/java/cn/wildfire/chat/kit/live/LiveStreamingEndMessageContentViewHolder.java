/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.message.viewholder.NormalMessageContentViewHolder;
import cn.wildfirechat.message.LiveStreamingEndMessageContent;

@MessageContentType(value = {
        LiveStreamingEndMessageContent.class,
})
public class LiveStreamingEndMessageContentViewHolder extends NormalMessageContentViewHolder {

    public LiveStreamingEndMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBind(UiMessage message) {
        // Static layout — no dynamic data needed
    }
}
