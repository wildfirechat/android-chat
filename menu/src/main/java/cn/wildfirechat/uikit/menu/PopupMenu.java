/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.uikit.menu;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class PopupMenu {

    private PopupWindow mWindow;
    private final Context mContext;
    private final int[] mTargetViewLocation = new int[2];
    private List<Pair<Integer, String>> mMenuItems;
    private PopupMenuAdapter listAdapter;
    private RecyclerView rvContent;
    private ImageView ivArrow;
    private ImageView ivArrowUp;
    private int popBgResource;
    private int popArrowImg;
    private int popAnimationStyle;
    private final OnMenuItemClickListener mMenuItemClickListener;

    @SuppressLint("InflateParams")
    public PopupMenu(Context context, List<Pair<Integer, String>> menuItems,
                     OnMenuItemClickListener menuItemClickListener) {
        this(context, menuItems, menuItemClickListener, 0, 0, 0);
    }

    public PopupMenu(Context context, List<Pair<Integer, String>> menuItems,
                     OnMenuItemClickListener menuItemClickListener,
                     int popBgResource, int popArrowImg,
                     int popAnimationStyle) {

        mContext = context;
        mMenuItems = menuItems;
        mMenuItemClickListener = menuItemClickListener;
        this.popBgResource = popBgResource;
        this.popArrowImg = popArrowImg;
        this.popAnimationStyle = popAnimationStyle;
    }

    public void showAsListMenu(View anchorView) {
        this.showAsListMenu(anchorView, 0, 0);
    }

    /**
     * @param anchorView 锚点view
     * @param xOffset    x轴偏移量，在自动计算的基础上进行的偏移
     * @param yOffset    y轴偏移量，在自动计算的基础上进行的偏移
     */
    public void showAsListMenu(View anchorView, int xOffset, int yOffset) {
        this.initContentView(mContext, false);
        listAdapter = new PopupMenuAdapter(mContext, mMenuItems, false);
        listAdapter.setOnclickItemListener(position -> {
            dismiss();
            if (mMenuItemClickListener != null) {
                mMenuItemClickListener.onClick(position);
            }
        });

        rvContent.setAdapter(listAdapter);
        int deviceWidth = Display.getScreenMetrics(mContext).x;
        int deviceHeight = Display.getScreenMetrics(mContext).y;
        int size = listAdapter.getItemCount();
        rvContent.setLayoutManager(new LinearLayoutManager(mContext));

        // 设置分割线
        Drawable dividerDrawable = ContextCompat.getDrawable(mContext, R.drawable.divider);
        LitemMenuDividerItemDecoration litemMenuDividerItemDecoration = new LitemMenuDividerItemDecoration(dividerDrawable);
        rvContent.addItemDecoration(litemMenuDividerItemDecoration);

        int contentViewWidth = Display.dip2px(mContext, 140) + Display.dip2px(mContext, 24)/* margin star and end*/;
        int row = size;
        int contentViewHeight = Display.dip2px(mContext, 50) * row + Display.dip2px(mContext, 7)/* arrow image*/;


        anchorView.getLocationInWindow(mTargetViewLocation);
        int posX;
        int targetViewStartX = mTargetViewLocation[0];
        int targetViewCenterX = targetViewStartX + anchorView.getWidth() / 2;
        // 默认放在anchorView的上面
        int posY = mTargetViewLocation[1] - contentViewHeight + yOffset;

        ImageView arrow = ivArrow;
        // 状态栏高度
        if (posY < Display.dip2px(mContext, 60)) {
            posY = mTargetViewLocation[1] + anchorView.getHeight() + yOffset;
            if (posY > deviceHeight - contentViewHeight) {
                posY = deviceHeight - contentViewHeight;
            }

            ivArrow.setVisibility(View.GONE);
            ivArrowUp.setVisibility(View.VISIBLE);
            arrow = ivArrowUp;
        } else {
            ivArrow.setVisibility(View.VISIBLE);
            ivArrowUp.setVisibility(View.GONE);
        }

        posX = (targetViewStartX + anchorView.getWidth() / 2) - contentViewWidth / 2 + xOffset;

        if (posX <= 0) {
            posX = Display.dip2px(mContext, 10);
            mWindow.showAtLocation(anchorView, Gravity.LEFT | Gravity.TOP, posX, posY);
        } else if (posX + contentViewWidth >= deviceWidth - Display.dip2px(mContext, 10)) {
            posX = Display.dip2px(mContext, 10);
            mWindow.showAtLocation(anchorView, Gravity.RIGHT | Gravity.TOP, posX, posY);
        } else {
            mWindow.showAtLocation(anchorView, Gravity.LEFT | Gravity.TOP, posX, posY);
        }

        ImageView finalArrow = arrow;
        mWindow.getContentView().post(() -> {
            int[] location = new int[2];
            mWindow.getContentView().getLocationOnScreen(location);
            float arrowTranslationX = targetViewCenterX - location[0] - Display.dip2px(mContext, 10);
            finalArrow.setTranslationX(arrowTranslationX);
        });
    }


    public void showAsGridMenu(View anchorView, int spanCount) {
        this.initContentView(mContext, true);
        listAdapter = new PopupMenuAdapter(mContext, mMenuItems, true);
        listAdapter.setOnclickItemListener(position -> {
            dismiss();
            if (mMenuItemClickListener != null) {
                mMenuItemClickListener.onClick(position);
            }
        });

        rvContent.setAdapter(listAdapter);
        int deviceWidth = Display.getScreenMetrics(mContext).x;
        int deviceHeight = Display.getScreenMetrics(mContext).y;
        int size = listAdapter.getItemCount();
        if (size > spanCount) {
            rvContent.setLayoutManager(new GridLayoutManager(mContext,
                spanCount, GridLayoutManager.VERTICAL, false));
        } else {
            rvContent.setLayoutManager(new GridLayoutManager(mContext,
                size, GridLayoutManager.VERTICAL, false));
        }

        // 设置分割线
        Drawable dividerDrawable = ContextCompat.getDrawable(mContext, R.drawable.divider);
        GridMenuDividerItemDecoration gridMenuDividerItemDecoration = new GridMenuDividerItemDecoration(dividerDrawable);
        rvContent.addItemDecoration(gridMenuDividerItemDecoration);

        int contentViewWidth = Display.dip2px(mContext, 60) * Math.min(size, spanCount) + Display.dip2px(mContext, 24)/* margin star and end*/;
        int row = (int) Math.ceil((double) size / spanCount);
        int contentViewHeight = Display.dip2px(mContext, 80) * row + Display.dip2px(mContext, 7)/* arrow image*/;


        anchorView.getLocationInWindow(mTargetViewLocation);
        int posX;
        int targetViewStartX = mTargetViewLocation[0];
        int targetViewCenterX = targetViewStartX + anchorView.getWidth() / 2;
        // 默认放在anchorView的上面
        int posY = mTargetViewLocation[1] - contentViewHeight;

        ImageView arrow = ivArrow;
        // 状态栏高度
        if (posY < Display.dip2px(mContext, 60)) {
            posY = mTargetViewLocation[1] + anchorView.getHeight();
            if (posY > deviceHeight - contentViewHeight) {
                posY = deviceHeight - contentViewHeight;
            }

            ivArrow.setVisibility(View.GONE);
            ivArrowUp.setVisibility(View.VISIBLE);
            arrow = ivArrowUp;
        } else {
            ivArrow.setVisibility(View.VISIBLE);
            ivArrowUp.setVisibility(View.GONE);
        }

        posX = (targetViewStartX + anchorView.getWidth() / 2) - contentViewWidth / 2;

        if (posX <= 0) {
            posX = Display.dip2px(mContext, 10);
            mWindow.showAtLocation(anchorView, Gravity.LEFT | Gravity.TOP, posX, posY);
        } else if (posX + contentViewWidth >= deviceWidth - Display.dip2px(mContext, 10)) {
            posX = Display.dip2px(mContext, 10);
            mWindow.showAtLocation(anchorView, Gravity.RIGHT | Gravity.TOP, posX, posY);
        } else {
            mWindow.showAtLocation(anchorView, Gravity.LEFT | Gravity.TOP, posX, posY);
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

    private void initContentView(Context context, boolean gridMenu) {
        View contentView = LayoutInflater.from(context).inflate(gridMenu ? R.layout.popup_grid_menu : R.layout.popup_list_menu, null);
        rvContent = contentView.findViewById(R.id.rv_content);
        ivArrow = contentView.findViewById(R.id.iv_arrow);
        ivArrowUp = contentView.findViewById(R.id.iv_arrow_up);
        if (popBgResource != 0) {
            rvContent.setBackgroundResource(popBgResource);
        }
        if (popArrowImg != 0) {
            ivArrow.setBackgroundResource(popArrowImg);
        }

        mWindow = new PopupWindow(contentView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mWindow.setClippingEnabled(false);
        mWindow.setOutsideTouchable(true);
        mWindow.setFocusable(true);

        if (popAnimationStyle != 0) {
            mWindow.setAnimationStyle(popAnimationStyle);
        }
    }

}