/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.uikit.menu;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GridMenuDividerItemDecoration extends RecyclerView.ItemDecoration {

    private final Drawable mDivider;

    public GridMenuDividerItemDecoration(Drawable divider) {
        mDivider = divider;
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int childCount = parent.getChildCount();
        int spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();

        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            // Draw right divider
//            if ((i + 1) % spanCount != 0) {
//                int left = child.getRight();
//                int right = left + mDivider.getIntrinsicWidth();
//                int top = child.getTop();
//                int bottom = child.getBottom();
//                mDivider.setBounds(left, top, right, bottom);
//                mDivider.draw(canvas);
//            }

//             Draw bottom divider
            int left = child.getLeft();
            int right = child.getRight();
            int top = child.getBottom();
            int bottom = top + mDivider.getIntrinsicHeight();
            if (i % spanCount == 0) {
                left += Display.dip2px(parent.getContext(), 12);
            }

            if ((i + 1) % spanCount == 0) {
                right -= Display.dip2px(parent.getContext(), 12);
            }
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(canvas);
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();
        int position = parent.getChildAdapterPosition(view);

        if ((position + 1) % spanCount != 0) {
            outRect.right = mDivider.getIntrinsicWidth();
        }
        outRect.bottom = mDivider.getIntrinsicHeight();
    }
}
