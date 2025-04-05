/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package com.noober.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HorizontalContextMenu {

    private final PopupWindow mWindow;
    private final Context mContext;
    private final View mTargetView;
    private final int[] mTargetViewLocation = new int[2];
    private final int mWidth;
    private final int mHeight;
    private final HorizontalContextMenuAdapter listAdapter;
    private final RecyclerView rvContent;
    private final View mContentView;
    private final ImageView ivArrow;
    private final ImageView ivArrowUp;
    private final int mPopSpanCount;

    @SuppressLint("InflateParams")
    public HorizontalContextMenu(Context context, View targetView, List<Pair<Integer, String>> menuItems,
                                 OnMenuItemClickListener menuItemClickListener,
                                 int popSpanCount, int popBgResource, int popArrowImg,
                                 int popAnimationStyle) {

        View contentView = LayoutInflater.from(context).inflate(R.layout.horizontal_context_menu, null);
        rvContent = contentView.findViewById(R.id.rv_content);
        ivArrow = contentView.findViewById(R.id.iv_arrow);
        ivArrowUp = contentView.findViewById(R.id.iv_arrow_up);
        mContentView = contentView;
        mPopSpanCount = popSpanCount;
        mContext = context;
        mTargetView = targetView;

        if (popBgResource != 0) {
            rvContent.setBackgroundResource(popBgResource);
        }
        if (popArrowImg != 0) {
            ivArrow.setBackgroundResource(popArrowImg);
        }

        int size = menuItems.size();
        mWidth = Display.dip2px(context, 66) * Math.min(size, mPopSpanCount);
        int row = (int) Math.ceil((double) size / mPopSpanCount);
        mHeight = Display.dip2px(context, 85) * row;

        mWindow = new PopupWindow(contentView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mWindow.setClippingEnabled(false);
        mWindow.setOutsideTouchable(true);

        if (popAnimationStyle != 0) {
            mWindow.setAnimationStyle(popAnimationStyle);
        }

        listAdapter = new HorizontalContextMenuAdapter(context, menuItems);
        listAdapter.setOnclickItemListener(position -> {
            dismiss();
            menuItemClickListener.onClick(position);
        });
        rvContent.setAdapter(listAdapter);
    }

    public void show() {
        int deviceWidth = Display.getScreenMetrics(mContext).x;
        int deviceHeight = Display.getScreenMetrics(mContext).y;
        int size = listAdapter.getItemCount();
        if (size > mPopSpanCount) {
            rvContent.setLayoutManager(new GridLayoutManager(mContext,
                mPopSpanCount, GridLayoutManager.VERTICAL, false));
        } else {
            rvContent.setLayoutManager(new GridLayoutManager(mContext,
                size, GridLayoutManager.VERTICAL, false));
        }

        // 设置分割线
        Drawable dividerDrawable = ContextCompat.getDrawable(mContext, R.drawable.divider);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(dividerDrawable);
        rvContent.addItemDecoration(dividerItemDecoration);

        mTargetView.getLocationInWindow(mTargetViewLocation);
        int posX;
        int targetViewStartX = mTargetViewLocation[0];
        int targetViewCenterX = targetViewStartX + mTargetView.getWidth() / 2;
        // 默认放在targetView的上面
        int posY = mTargetViewLocation[1] - mHeight;

        ImageView arrow = ivArrow;
        // 状态栏高度
        if (posY < Display.dip2px(mContext, 60)) {
            posY = mTargetViewLocation[1] + mTargetView.getHeight();
            if (posY > deviceHeight - mHeight) {
                posY = deviceHeight - mHeight;
            }

            ivArrow.setVisibility(View.GONE);
            ivArrowUp.setVisibility(View.VISIBLE);
            arrow = ivArrowUp;
        } else {
            ivArrow.setVisibility(View.VISIBLE);
            ivArrowUp.setVisibility(View.GONE);
        }

        posX = (targetViewStartX + mTargetView.getWidth() / 2) - mWidth / 2;

        if (posX <= 0) {
            posX = Display.dip2px(mContext, 10);
            mWindow.showAtLocation(mTargetView, Gravity.LEFT | Gravity.TOP, posX, posY);
        } else if (posX + mWidth >= deviceWidth - Display.dip2px(mContext, 10)) {
            posX = Display.dip2px(mContext, 10);
            mWindow.showAtLocation(mTargetView, Gravity.RIGHT | Gravity.TOP, posX, posY);
        } else {
            mWindow.showAtLocation(mTargetView, Gravity.LEFT | Gravity.TOP, posX, posY);
        }

        ImageView finalArrow = arrow;
        mWindow.getContentView().post(() -> {
            int[] location = new int[2];
            mWindow.getContentView().getLocationOnScreen(location);
            float arrowTranslationX = targetViewCenterX - location[0] - Display.dip2px(mContext, 10);
            finalArrow.setTranslationX(arrowTranslationX);
        });
    }

    public void dismiss() {
        mWindow.dismiss();
    }

    public boolean isShowing() {
        return mWindow.isShowing();
    }

}