/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.dsh.DshAnswerMessageContent;
import cn.wildfirechat.message.dsh.DshApprovalResultMessageContent;

/**
 * DSH 应答类消息（201 回答 / 203 审批结果）的摘要文本 ViewHolder。
 * <p>
 * 这两类是用户侧应答消息，按普通文本气泡渲染 content.digest()。
 * </p>
 */
@MessageContentType(value = {
    DshAnswerMessageContent.class,
    DshApprovalResultMessageContent.class
})
@EnableContextMenu
public class DshDigestMessageContentViewHolder extends NormalMessageContentViewHolder {

    public DshDigestMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBind(UiMessage message) {
        androidx.emoji2.widget.EmojiTextView contentTextView = itemView.findViewById(R.id.contentTextView);
        contentTextView.setText(message.message.content.digest(message.message));
        View refTextView = itemView.findViewById(R.id.refTextView);
        if (refTextView != null) {
            refTextView.setVisibility(View.GONE);
        }
    }
}
