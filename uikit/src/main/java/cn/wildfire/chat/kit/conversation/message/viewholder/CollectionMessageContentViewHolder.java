/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.collection.CollectionDetailActivity;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.CollectionEntry;
import cn.wildfirechat.message.CollectionMessageContent;

/**
 * 接龙消息ViewHolder
 * <p>
 * 用于在消息列表中展示接龙消息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@MessageContentType(CollectionMessageContent.class)
@EnableContextMenu
public class CollectionMessageContentViewHolder extends NormalMessageContentViewHolder {

    private static final int MAX_ENTRIES_TO_SHOW = 5;

    TextView titleTextView;
    TextView countTextView;
    TextView descTextView;
    LinearLayout entriesContainer;
    TextView emptyHintTextView;
    TextView moreTextView;
    TextView actionTextView;

    public CollectionMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        View contentView = itemView.findViewById(R.id.collectionContainer);
        if (contentView != null) {
            contentView.setOnClickListener(this::onClick);
        }
    }

    private void bindViews(View itemView) {
        titleTextView = itemView.findViewById(R.id.titleTextView);
        countTextView = itemView.findViewById(R.id.countTextView);
        descTextView = itemView.findViewById(R.id.descTextView);
        entriesContainer = itemView.findViewById(R.id.entriesContainer);
        emptyHintTextView = itemView.findViewById(R.id.emptyHintTextView);
        moreTextView = itemView.findViewById(R.id.moreTextView);
        actionTextView = itemView.findViewById(R.id.actionTextView);
    }

    @Override
    protected void onBind(UiMessage message) {
        CollectionMessageContent content = (CollectionMessageContent) message.message.content;

        // 设置标题
        titleTextView.setText(content.getTitle());

        // 设置参与人数
        int count = content.getParticipantCount();
        countTextView.setText(fragment.getString(R.string.collection_participant_count, count));

        // 设置描述
        if (content.getDesc() != null && !content.getDesc().isEmpty()) {
            descTextView.setText(content.getDesc());
            descTextView.setVisibility(View.VISIBLE);
        } else {
            descTextView.setVisibility(View.GONE);
        }

        // 清空并重新填充参与记录预览
        entriesContainer.removeAllViews();
        List<CollectionEntry> entries = content.getEntries();

        if (entries == null || entries.isEmpty()) {
            // 显示空提示
            emptyHintTextView.setVisibility(View.VISIBLE);
            moreTextView.setVisibility(View.GONE);
        } else {
            emptyHintTextView.setVisibility(View.GONE);

            // 显示前5条参与记录
            int displayCount = Math.min(entries.size(), MAX_ENTRIES_TO_SHOW);
            for (int i = 0; i < displayCount; i++) {
                CollectionEntry entry = entries.get(i);
                if (entry.getDeleted() == 0) {
                    View entryView = createEntryView(entry, i + 1);
                    entriesContainer.addView(entryView);
                }
            }

            // 显示更多提示
            int remaining = count - displayCount;
            if (remaining > 0) {
                moreTextView.setText(fragment.getString(R.string.collection_more_participants, remaining));
                moreTextView.setVisibility(View.VISIBLE);
            } else {
                moreTextView.setVisibility(View.GONE);
            }
        }

        // 操作按钮统一显示"参与接龙"，不根据用户是否参与改变
        int status = content.getStatus();
        if (status == 1) {
            actionTextView.setText(R.string.collection_status_ended);
            actionTextView.setTextColor(0xFF999999);
        } else if (status == 2) {
            actionTextView.setText(R.string.collection_status_cancelled);
            actionTextView.setTextColor(0xFF999999);
        } else {
            actionTextView.setText(R.string.collection_join_action);
            actionTextView.setTextColor(0xFF576b95);
        }
    }

    /**
     * 创建参与记录视图
     * 格式：1. 内容（支持多行）
     *
     * @param entry 参与记录
     * @param index 序号
     * @return 视图
     */
    private View createEntryView(CollectionEntry entry, int index) {
        TextView entryTextView = new TextView(fragment.getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        entryTextView.setLayoutParams(params);
        entryTextView.setPadding(0, dpToPx(2), 0, dpToPx(2));
        entryTextView.setTextSize(14);
        entryTextView.setTextColor(0xFF333333);
        entryTextView.setText(index + ". " + entry.getContent());
        entryTextView.setMaxLines(1);
        entryTextView.setEllipsize(TextUtils.TruncateAt.END);
        return entryTextView;
    }

    private int dpToPx(int dp) {
        float density = fragment.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    public void onClick(View view) {
        Intent intent = new Intent(fragment.getContext(), CollectionDetailActivity.class);
        intent.putExtra("message", message.message);
        fragment.startActivity(intent);
    }

    /**
     * 拷贝接龙内容到剪贴板
     * 格式与iOS一致：
     * 标题
     * 描述（如果有）
     * 1. 内容1
     * 2. 内容2
     * ...
     */
    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_COPY, confirm = false, priority = 12)
    public void copy(View itemView, UiMessage message) {
        ClipboardManager clipboardManager = (ClipboardManager) fragment.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }

        CollectionMessageContent content = (CollectionMessageContent) message.message.content;
        StringBuilder copyText = new StringBuilder();

        // 标题
        copyText.append(content.getTitle());
        copyText.append("\n");

        // 描述（如果有）
        if (content.getDesc() != null && !content.getDesc().isEmpty()) {
            copyText.append(content.getDesc());
            copyText.append("\n");
        }

        // 参与项
        List<CollectionEntry> entries = content.getEntries();
        if (entries != null && !entries.isEmpty()) {
            for (int i = 0; i < entries.size(); i++) {
                CollectionEntry entry = entries.get(i);
                if (entry.getDeleted() == 0) {
                    copyText.append(i + 1).append(". ").append(entry.getContent());
                    copyText.append("\n");
                }
            }
        }

        ClipData clipData = ClipData.newPlainText("collectionContent", copyText.toString().trim());
        clipboardManager.setPrimaryClip(clipData);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_COPY.equals(tag)) {
            return context.getString(R.string.message_copy);
        }
        return super.contextMenuTitle(context, tag);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        // 接龙消息支持拷贝
        if (MessageContextMenuItemTags.TAG_COPY.equals(tag)) {
            return false;
        }
        if (MessageContextMenuItemTags.TAG_FORWARD.equals(tag)) {
            return true;
        }
        return super.contextMenuItemFilter(uiMessage, tag);
    }
}
