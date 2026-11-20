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
import cn.wildfirechat.message.agent.AgentAnswerMessageContent;
/**
 * Agent 用户回答（201）的摘要文本 ViewHolder。
 * <p>
 * 201 按普通文本气泡渲染 content.digest()；203 审批结果已改为 Transparent，
 * 不落库、不显示，审批完成只更新原 202 审批卡片。
 * </p>
 */
@MessageContentType(value = {
    AgentAnswerMessageContent.class
})
@EnableContextMenu
public class AgentDigestMessageContentViewHolder extends NormalMessageContentViewHolder {

    public AgentDigestMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
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
