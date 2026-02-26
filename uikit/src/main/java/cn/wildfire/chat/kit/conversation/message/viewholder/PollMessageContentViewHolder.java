/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.PollMessageContent;

/**
 * 投票消息内容ViewHolder
 * <p>
 * 用于显示投票创建消息的Cell
 * </p>
 */
@MessageContentType(PollMessageContent.class)
@EnableContextMenu
public class PollMessageContentViewHolder extends NormalMessageContentViewHolder {
    TextView titleTextView;
    TextView descTextView;
    TextView infoTextView;
    TextView actionTextView;

    private PollMessageContent pollMessageContent;

    public PollMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.pollMessageContentItemView).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        titleTextView = itemView.findViewById(R.id.titleTextView);
        descTextView = itemView.findViewById(R.id.descTextView);
        infoTextView = itemView.findViewById(R.id.infoTextView);
        actionTextView = itemView.findViewById(R.id.actionTextView);
    }

    // 固定的Cell宽度
    private static final int POLL_CELL_WIDTH = 240; // dp
    
    @Override
    protected void onBind(UiMessage message) {
        pollMessageContent = (PollMessageContent) message.message.content;
        
        // 确保Cell宽度固定
        View contentView = itemView.findViewById(R.id.pollMessageContentItemView);
        if (contentView != null) {
            ViewGroup.LayoutParams params = contentView.getLayoutParams();
            if (params != null) {
                params.width = (int) (POLL_CELL_WIDTH * fragment.getResources().getDisplayMetrics().density);
                contentView.setLayoutParams(params);
            }
        }
        
        // 设置标题
        String title = "🗳️ " + pollMessageContent.getTitle();
        titleTextView.setText(title);
        
        // 设置描述
        if (!TextUtils.isEmpty(pollMessageContent.getDesc())) {
            descTextView.setText(pollMessageContent.getDesc());
            descTextView.setVisibility(View.VISIBLE);
        } else {
            descTextView.setVisibility(View.GONE);
        }
        
        // 设置状态信息
        String statusText;
        if (pollMessageContent.getEndTime() > 0 && pollMessageContent.getEndTime() < message.message.serverTime) {
            statusText = fragment.getString(R.string.poll_ended);
        } else if (pollMessageContent.getStatus() == 1) {
            statusText = fragment.getString(R.string.poll_ended);
        } else {
            statusText = fragment.getString(R.string.poll_in_progress);
        }
        
        String typeText = pollMessageContent.getAnonymous() == 1 
            ? fragment.getString(R.string.anonymous_poll) 
            : fragment.getString(R.string.named_poll);
        
        String infoText = statusText + " · " + typeText;
        infoTextView.setText(infoText);
        
        // 设置操作按钮文字
        actionTextView.setText(R.string.click_to_vote);
    }

    public void onClick(View view) {
        // 跳转到投票详情页，传递message以支持投票场景
        Intent intent = cn.wildfire.chat.kit.poll.activity.PollDetailActivity
            .buildIntent(fragment.getContext(), message.message,
                Long.parseLong(pollMessageContent.getPollId()),
                pollMessageContent.getGroupId());
        fragment.startActivity(intent);
    }
}
