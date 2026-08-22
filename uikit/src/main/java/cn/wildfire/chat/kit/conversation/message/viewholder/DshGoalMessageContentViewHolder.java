/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.dsh.DshGoalMessageContent;

/**
 * DSH 目标进度卡片（206），纯展示：phase 彩色徽标 + objective + 已执行轮数。
 */
@MessageContentType(DshGoalMessageContent.class)
@EnableContextMenu
public class DshGoalMessageContentViewHolder extends NormalMessageContentViewHolder {

    TextView phaseBadgeTextView;
    TextView objectiveTextView;
    TextView roundsTextView;

    public DshGoalMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        phaseBadgeTextView = itemView.findViewById(R.id.dshPhaseBadgeTextView);
        objectiveTextView = itemView.findViewById(R.id.dshObjectiveTextView);
        roundsTextView = itemView.findViewById(R.id.dshRoundsTextView);
    }

    @Override
    protected void onBind(UiMessage message) {
        DshGoalMessageContent content = (DshGoalMessageContent) message.message.content;

        String phase = content.getPhase();
        phaseBadgeTextView.setText(phaseText(phase));
        Drawable badge = phaseBadgeTextView.getBackground().mutate();
        badge.setTint(phaseColor(phase));

        objectiveTextView.setText(content.getObjective());
        roundsTextView.setText("已执行 " + content.getRoundsStarted() + " 轮");
    }

    private String phaseText(String phase) {
        switch (phase == null ? "" : phase) {
            case "active":
                return "进行中";
            case "paused":
                return "已暂停";
            case "blocked":
                return "受阻";
            case "complete":
                return "已完成";
            default:
                return phase;
        }
    }

    private int phaseColor(String phase) {
        switch (phase == null ? "" : phase) {
            case "active":
                return Color.parseColor("#22c55e");
            case "paused":
                return Color.parseColor("#94a3b8");
            case "blocked":
                return Color.parseColor("#E64340");
            case "complete":
                return Color.parseColor("#3B62E0"); // 与 @color/colorPrimary 一致
            default:
                return Color.parseColor("#94a3b8");
        }
    }
}
