/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.remote.ChatManager;

@MessageContentType(RecallMessageContent.class)
@EnableContextMenu
public class RecallMessageContentViewHolder extends NotificationMessageContentViewHolder {
    TextView notificationTextView;
    TextView reeditTextView;

    private RecallMessageContent content;

    public RecallMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
       itemView.findViewById(R.id.reeditTextView).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        notificationTextView =itemView.findViewById(R.id.notificationTextView);
        reeditTextView =itemView.findViewById(R.id.reeditTextView);
    }

    @Override
    public void onBind(UiMessage message, int position) {
        super.onBind(message, position);
        content = (RecallMessageContent) message.message.content;
        notificationTextView.setText(message.message.digest());
        long delta = ChatManager.Instance().getServerDeltaTime();
        long now = System.currentTimeMillis();
        if (content.getOriginalContentType() == cn.wildfirechat.message.core.MessageContentType.ContentType_Text
            && ((NotificationMessageContent) message.message.content).fromSelf
            && now - (message.message.serverTime - delta) < Config.RECALL_REEDIT_TIME_LIMIT * 1000) {
            reeditTextView.setVisibility(View.VISIBLE);
        } else {
            reeditTextView.setVisibility(View.GONE);
        }
    }

    public void onClick(View view) {
        fragment.setInputText(content.getOriginalSearchableContent());
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String itemTitle) {
        return false;
    }

}
