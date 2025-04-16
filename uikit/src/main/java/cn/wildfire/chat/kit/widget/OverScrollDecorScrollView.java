/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class OverScrollDecorScrollView extends ScrollView {
    public OverScrollDecorScrollView(Context context) {
        super(context);
    }

    public OverScrollDecorScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setupOverScrollDecor();
    }

    public OverScrollDecorScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setupOverScrollDecor();
    }

    public OverScrollDecorScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.setupOverScrollDecor();
    }

    private void setupOverScrollDecor() {
        OverScrollDecoratorHelper.setUpOverScroll(this);
        setScrollBarSize(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredHeightBefore = getMeasuredHeight();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeightAfter = getMeasuredHeight();

        // ScrollView's height was changed
        if (measuredHeightBefore != measuredHeightAfter) {
            // update containers padding if needed
            updateContentPadding(measuredHeightAfter);
        }

        // child view's were updated, we need to remeasure
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void updateContentPadding(int scrollViewMeasuredHeight) {
        View contentView = getChildAt(0);
        int availableSpace = scrollViewMeasuredHeight - contentView.getMeasuredHeight() + 2;
        if (availableSpace > 0) {
            // 顶部对齐，所有额外空间都以底部padding形式添加
            contentView.setPadding(
                0, 0,    // padding top 为0，实现顶部对齐
                0, availableSpace);   // padding bottom 包含所有可用空间
        }
    }
}
