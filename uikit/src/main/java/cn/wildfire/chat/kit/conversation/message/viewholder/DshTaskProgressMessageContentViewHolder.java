/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.dsh.AgentTaskProgressMessageContent;

/**
 * DSH 任务进度卡片（208），纯展示：
 * 标题「🧩 任务进度」+ 摘要角标（共 N 个 · M 运行中 / 全部完成 / N 失败），
 * 每行 = 状态图标 + 标签（label 或 id 短前缀）+ 状态文字（失败附原因）。
 */
@MessageContentType(AgentTaskProgressMessageContent.class)
@EnableContextMenu
public class DshTaskProgressMessageContentViewHolder extends NormalMessageContentViewHolder {

    private static final int SUMMARY_BADGE_COLOR = Color.parseColor("#4f8ff7"); // 与 PC 端摘要角标一致

    TextView summaryTextView;
    TextView emptyTextView;
    LinearLayout listContainer;

    public DshTaskProgressMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        summaryTextView = itemView.findViewById(R.id.dshTaskProgressSummaryTextView);
        emptyTextView = itemView.findViewById(R.id.dshTaskProgressEmptyTextView);
        listContainer = itemView.findViewById(R.id.dshTaskProgressListContainer);
    }

    @Override
    protected void onBind(UiMessage message) {
        AgentTaskProgressMessageContent content = (AgentTaskProgressMessageContent) message.message.content;

        String summary = content.getSummary();
        if (TextUtils.isEmpty(summary)) {
            summaryTextView.setVisibility(View.GONE);
        } else {
            summaryTextView.setVisibility(View.VISIBLE);
            summaryTextView.setText(summary);
            Drawable badge = summaryTextView.getBackground().mutate();
            badge.setTint(SUMMARY_BADGE_COLOR);
        }

        listContainer.removeAllViews();
        JSONArray tasks = content.getTasks();
        int total = tasks != null ? tasks.length() : 0;
        if (total == 0) {
            emptyTextView.setVisibility(View.VISIBLE);
            listContainer.setVisibility(View.GONE);
            return;
        }
        emptyTextView.setVisibility(View.GONE);
        listContainer.setVisibility(View.VISIBLE);
        for (int i = 0; i < total; i++) {
            AgentTaskProgressMessageContent.Task task = AgentTaskProgressMessageContent.Task.from(tasks.optJSONObject(i));
            if (task != null) {
                listContainer.addView(buildTaskRow(task));
            }
        }
    }

    private View buildTaskRow(AgentTaskProgressMessageContent.Task task) {
        View row = LayoutInflater.from(fragment.getContext()).inflate(R.layout.dsh_task_progress_item, listContainer, false);

        TextView iconTextView = row.findViewById(R.id.dshTaskProgressIconTextView);
        TextView labelTextView = row.findViewById(R.id.dshTaskProgressLabelTextView);
        TextView metaTextView = row.findViewById(R.id.dshTaskProgressMetaTextView);

        iconTextView.setText(statusIcon(task.status));
        labelTextView.setText(task.displayLabel());
        String meta = statusText(task.status);
        if (!TextUtils.isEmpty(task.reason)) {
            meta += " · " + task.reason;
        }
        metaTextView.setText(meta);
        return row;
    }

    private String statusIcon(String status) {
        switch (status == null ? "" : status) {
            case "running":
                return "⏳";
            case "done":
            case "completed":
                return "✅";
            case "failed":
                return "❌";
            case "killed":
                return "⛔";
            default:
                return "⚪";
        }
    }

    private String statusText(String status) {
        switch (status == null ? "" : status) {
            case "running":
                return "运行中";
            case "done":
            case "completed":
                return "已完成";
            case "failed":
                return "失败";
            case "killed":
                return "已终止";
            default:
                return TextUtils.isEmpty(status) ? "" : status;
        }
    }
}
