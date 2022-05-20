/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import cn.wildfire.chat.kit.R;

public class SimpleLabelView extends LinearLayout {
    private TextView titleTextView;
    private TextView descTextView;

    private String title;
    private String desc;

    public SimpleLabelView(Context context) {
        super(context);
        init(context, null);
    }

    public SimpleLabelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SimpleLabelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SimpleLabelView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.conversation_item_rich_notification_data_item, this);
        titleTextView = view.findViewById(R.id.titleTextView);
        descTextView = view.findViewById(R.id.descTextView);

        if (!TextUtils.isEmpty(title)) {
            titleTextView.setText(title);
        }
        if (!TextUtils.isEmpty(desc)) {
            descTextView.setText(desc);
        }
    }

    public void setTitle(String title) {
        this.title = title;
        if (titleTextView != null) {
            titleTextView.setText(title);
        }
    }

    public void setDesc(String desc) {
        this.desc = desc;
        if (descTextView != null) {
            if (TextUtils.isEmpty(desc)) {
                descTextView.setVisibility(GONE);
            } else {
                descTextView.setVisibility(VISIBLE);
                descTextView.setText(desc);
            }
        }
    }

    public void setDesc(String desc, String color) {
        this.setDesc(desc);
        if (!TextUtils.isEmpty(desc)) {
            try {
                descTextView.setTextColor(Color.parseColor(color));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}