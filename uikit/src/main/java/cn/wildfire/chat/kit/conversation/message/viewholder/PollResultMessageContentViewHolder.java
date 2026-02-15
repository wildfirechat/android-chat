/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.PollResultMessageContent;

/**
 * 投票结果消息内容ViewHolder
 * <p>
 * 用于显示投票结果消息的Cell
 * </p>
 */
@MessageContentType(PollResultMessageContent.class)
@EnableContextMenu
public class PollResultMessageContentViewHolder extends NormalMessageContentViewHolder {
    TextView titleTextView;
    TextView winnerTextView;
    TextView infoTextView;

    private PollResultMessageContent pollResultMessageContent;

    public PollResultMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }
    
    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.pollResultMessageContentItemView).setOnClickListener(this::onClick);
    }
    
    public void onClick(View view) {
        // 跳转到投票详情页
        Intent intent = new Intent(fragment.getContext(), cn.wildfire.chat.kit.poll.activity.PollDetailActivity.class);
        intent.putExtra("pollId", Long.parseLong(pollResultMessageContent.getPollId()));
        intent.putExtra("groupId", pollResultMessageContent.getGroupId());
        fragment.startActivity(intent);
    }

    private void bindViews(View itemView) {
        titleTextView = itemView.findViewById(R.id.titleTextView);
        winnerTextView = itemView.findViewById(R.id.winnerTextView);
        infoTextView = itemView.findViewById(R.id.infoTextView);
    }

    @Override
    protected void onBind(UiMessage message) {
        pollResultMessageContent = (PollResultMessageContent) message.message.content;
        
        // 设置标题
        String title = "📊 " + pollResultMessageContent.getTitle();
        titleTextView.setText(title);
        
        // 设置获胜选项
        List<String> winningOptionTexts = pollResultMessageContent.getWinningOptionTexts();
        if (winningOptionTexts != null && !winningOptionTexts.isEmpty()) {
            String winnerText = TextUtils.join("、", winningOptionTexts);
            String winnerLabel = "🏆 " + fragment.getString(R.string.winner) + ": " + winnerText;
            winnerTextView.setText(winnerLabel);
            winnerTextView.setVisibility(View.VISIBLE);
        } else {
            winnerTextView.setVisibility(View.GONE);
        }
        
        // 设置统计信息（使用voterCount显示实际参与人数）
        String infoText = String.format(fragment.getString(R.string.poll_result_info), pollResultMessageContent.getVoterCount());
        infoTextView.setText(infoText);
    }
}
