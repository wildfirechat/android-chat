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

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
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
        // 跳转到投票详情页，传递message以支持投票场景
        Intent intent = cn.wildfire.chat.kit.poll.activity.PollDetailActivity
            .buildIntent(fragment.getContext(), message.message,
                Long.parseLong(pollResultMessageContent.getPollId()),
                pollResultMessageContent.getGroupId());
        // 走 WfcPageCompat：平板上投票详情压到会话所在的那条右栏栈上，而不是整屏跳出去
        WfcPageCompat.startPage(fragment, intent);
    }

    private void bindViews(View itemView) {
        titleTextView = itemView.findViewById(R.id.titleTextView);
        winnerTextView = itemView.findViewById(R.id.winnerTextView);
        infoTextView = itemView.findViewById(R.id.infoTextView);
    }

    // 固定的Cell宽度
    private static final int POLL_CELL_WIDTH = 240; // dp
    
    @Override
    protected void onBind(UiMessage message) {
        pollResultMessageContent = (PollResultMessageContent) message.message.content;
        
        // 确保Cell宽度固定
        View contentView = itemView.findViewById(R.id.pollResultMessageContentItemView);
        if (contentView != null) {
            ViewGroup.LayoutParams params = contentView.getLayoutParams();
            if (params != null) {
                params.width = (int) (POLL_CELL_WIDTH * fragment.getResources().getDisplayMetrics().density);
                contentView.setLayoutParams(params);
            }
        }
        
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
