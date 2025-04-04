/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package com.noober.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HorizontalContextMenu {

    private final PopupWindow mWindow;
    private final Context mContext;
    private final View mTargetView;
    private final int[] mTempCoors = new int[2];
    private final int mWidth;
    private final int mHeight;
    private final HorizontalContextMenuAdapter listAdapter;
    private final RecyclerView rvContent;
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

        mTargetView.getLocationInWindow(mTempCoors);
        Log.d("jyj", mTempCoors[0] + " " + mTempCoors[1]);
        int posX;
        int startX = mTempCoors[0];
        // 默认放在targetView的上面
        int posY = mTempCoors[1] - mHeight;

        ImageView arrow = ivArrow;
        // 状态栏高度
        if (posY < Display.dip2px(mContext, 60)) {
            posY = mTempCoors[1] + mTargetView.getHeight();
            if (posY > deviceHeight - mHeight) {
                posY = deviceHeight - mHeight;
            }

            ivArrow.setVisibility(View.GONE);
            ivArrowUp.setVisibility(View.VISIBLE);
            arrow = ivArrowUp;
        }

        posX = (startX + (mTempCoors[0] + mTargetView.getWidth())) / 2 - mWidth / 2;

        if (posX <= 0) {
            posX = Display.dip2px(mContext, 20);
        } else if (posX + mWidth >= deviceWidth - Display.dip2px(mContext, 20)) {
            posX = deviceWidth - mWidth - Display.dip2px(mContext, 20);
        }

        mWindow.showAtLocation(mTargetView, Gravity.NO_GRAVITY, posX, posY);

        int targetViewCenterX = mTempCoors[0] + mTargetView.getWidth() / 2;
        if (targetViewCenterX > posX) {
            arrow.setTranslationX(targetViewCenterX - posX - Display.dip2px(mContext, 20));
        } else {
            // never
            arrow.setTranslationX(posX - targetViewCenterX - arrow.getWidth());
        }

    }

    public void dismiss() {
        mWindow.dismiss();
    }

    public boolean isShowing() {
        return mWindow.isShowing();
    }

}