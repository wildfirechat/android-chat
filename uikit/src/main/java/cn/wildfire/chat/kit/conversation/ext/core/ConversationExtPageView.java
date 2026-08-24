/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.Conversation;

public class ConversationExtPageView extends LinearLayout implements View.OnClickListener {
    private OnExtViewClickListener listener;
    private int pageIndex;
    private Conversation conversation;
    public static final int EXT_PER_PAGE = 8;

    public ConversationExtPageView(Context context) {
        super(context);
        init(context);
    }

    public ConversationExtPageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConversationExtPageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ConversationExtPageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.conversation_ext_layout, this, false);
        addView(view);
    }

    public void updateExtViews(List<ConversationExt> exts) {
        updateExtViews(exts, null);
    }

    public void updateExtViews(List<ConversationExt> exts, Conversation conversation) {
        this.conversation = conversation;
        int[][] states = new int[][]{
            new int[]{android.R.attr.state_pressed},  // pressed
            new int[]{}
        };

        for (int index = 0; index < exts.size(); index++) {
            ImageView iconImageView = findViewWithTag("icon_" + index);
            iconImageView.setImageResource(exts.get(index).iconResId());

            TextView titleTextView = findViewWithTag("title_" + index);
            titleTextView.setText(exts.get(index).title(getContext()));
            // 置灰禁用：icon 半透明、标题变灰、点击不响应（如 AI 不在线时的 "AI 会话设置"）
            if (exts.get(index).disabled(this.conversation)) {
                iconImageView.setAlpha(0.35f);
                titleTextView.setAlpha(0.35f);
                iconImageView.setOnClickListener(null);
            } else {
                iconImageView.setAlpha(1.0f);
                titleTextView.setAlpha(1.0f);
                iconImageView.setOnClickListener(this);
            }
        }

        if(exts.size() < EXT_PER_PAGE){
            for (int index = exts.size(); index < EXT_PER_PAGE; index++) {
                ImageView iconImageView = findViewWithTag("icon_" + index);
                iconImageView.setVisibility(GONE);
                TextView titleTextView = findViewWithTag("title_" + index);
                titleTextView.setVisibility(GONE);
            }
        }
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    @Override
    public void onClick(View v) {
        String tag = (String) v.getTag();
        int index = Integer.parseInt(tag.substring(tag.lastIndexOf("_") + 1));
        if (listener != null) {
            listener.onClick(pageIndex * EXT_PER_PAGE + index);
        }
    }

    public void setOnExtViewClickListener(OnExtViewClickListener listener) {
        this.listener = listener;
    }

    public interface OnExtViewClickListener {
        void onClick(int index);
    }
}
