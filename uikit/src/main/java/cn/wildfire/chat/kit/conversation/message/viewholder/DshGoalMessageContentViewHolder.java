/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.dsh.AgentGoalMessageContent;

/**
 * DSH 目标进度卡片（206），纯展示：阶段彩色徽标 + 目标标题 + 已执行轮数（+ ver:2 stage）。
 * <p>
 * 兼容 ver:2 目标消息：objective 缺失时用 title，phase 缺失时用 state，
 * stage 存在时追加一行「阶段：…」（如 "阶段：round 3"）。
 * </p>
 */
@MessageContentType(AgentGoalMessageContent.class)
@EnableContextMenu
public class DshGoalMessageContentViewHolder extends NormalMessageContentViewHolder {

    TextView phaseBadgeTextView;
    TextView objectiveTextView;
    TextView roundsTextView;
    TextView stageTextView;

    public DshGoalMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        phaseBadgeTextView = itemView.findViewById(R.id.dshPhaseBadgeTextView);
        objectiveTextView = itemView.findViewById(R.id.dshObjectiveTextView);
        roundsTextView = itemView.findViewById(R.id.dshRoundsTextView);
        stageTextView = itemView.findViewById(R.id.dshStageTextView);
    }

    @Override
    protected void onBind(UiMessage message) {
        AgentGoalMessageContent content = (AgentGoalMessageContent) message.message.content;

        // v1 phase 优先，缺失（ver:2）时回退 state
        String phase = content.getDisplayPhase();
        phaseBadgeTextView.setText(phaseText(phase));
        Drawable badge = phaseBadgeTextView.getBackground().mutate();
        badge.setTint(phaseColor(phase));

        // v1 objective 优先，缺失（ver:2）时回退 title
        objectiveTextView.setText(content.getDisplayTitle());
        roundsTextView.setText("已执行 " + content.getRoundsStarted() + " 轮");

        // ver:2 stage 文本（如 "round 3"），无则不占行
        String stage = content.getStage();
        if (TextUtils.isEmpty(stage)) {
            stageTextView.setVisibility(View.GONE);
        } else {
            stageTextView.setText("阶段：" + stage);
            stageTextView.setVisibility(View.VISIBLE);
        }
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
