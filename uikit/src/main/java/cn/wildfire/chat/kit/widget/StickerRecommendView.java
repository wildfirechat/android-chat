/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.lqr.emoji.StickerItem;
import com.lqr.emoji.StickerManager;

import java.util.List;

/**
 * 动态表情推荐视图
 * 当用户输入匹配的文本时，显示动态表情预览
 * 支持显示多个表情，可左右滑动查看
 */
public class StickerRecommendView extends FrameLayout {
    private LinearLayout mStickersContainer;
    private HorizontalScrollView mHorizontalScrollView;
    private List<StickerItem> mMatchedStickers;

    private static final int AUTO_HIDE_DELAY = 3000;  // 3秒自动隐藏

    public StickerRecommendView(Context context) {
        super(context);
        initView(context);
    }

    public StickerRecommendView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public StickerRecommendView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        // 创建水平滚动容器
        mHorizontalScrollView = new HorizontalScrollView(context);
        mHorizontalScrollView.setHorizontalScrollBarEnabled(false);

        // 创建表情容器
        mStickersContainer = new LinearLayout(context);
        mStickersContainer.setOrientation(LinearLayout.HORIZONTAL);
//        mStickersContainer.setPadding(dpToPx(context, 8), dpToPx(context, 8),
//                dpToPx(context, 8), dpToPx(context, 8));

        mHorizontalScrollView.addView(mStickersContainer);
        addView(mHorizontalScrollView);

        // 初始隐藏
        setVisibility(GONE);
    }

    /**
     * 显示匹配到的表情
     * @param stickers 匹配到的表情列表
     */
    public void showStickers(List<StickerItem> stickers) {
        loadStickersOnly(stickers);
        setVisibility(VISIBLE);
    }

    /**
     * 只加载表情数据，不启动自动隐藏定时器
     * @param stickers 匹配到的表情列表
     */
    public void loadStickersOnly(List<StickerItem> stickers) {
        mMatchedStickers = stickers;

        if (stickers == null || stickers.isEmpty()) {
            return;
        }

        // 清空现有视图
        mStickersContainer.removeAllViews();

        // 添加表情视图
        for (StickerItem item : stickers) {
            ImageView imageView = createStickerImageView(getContext(), item);
            mStickersContainer.addView(imageView);
        }
    }

    /**
     * 创建单个表情的 ImageView
     */
    private ImageView createStickerImageView(Context context, StickerItem item) {
        ImageView imageView = new ImageView(context);
        int size = dpToPx(context, 100);  // 100dp

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
//        params.setMargins(0, 0, dpToPx(context, 8), 0);  // 右边距 8dp
        imageView.setLayoutParams(params);
        imageView.setPadding(dpToPx(context, 8), dpToPx(context, 8),
                dpToPx(context, 8), dpToPx(context, 8));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // 加载表情图片
        String path = StickerManager.getInstance()
                .getStickerBitmapPath(item.getCategory(), item.getName());
        Glide.with(context)
                .load(path)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);

        // 设置点击事件
        imageView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onStickerRecommendSelected(item);
            }
        });

        return imageView;
    }

    /**
     * 获取当前表情数量
     */
    public int getStickerCount() {
        return mMatchedStickers != null ? mMatchedStickers.size() : 0;
    }

    /**
     * dp 转 px
     */
    private int dpToPx(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    // 回调接口
    public interface OnStickerRecommendListener {
        void onStickerRecommendSelected(StickerItem sticker);
    }

    private OnStickerRecommendListener mListener;

    public void setOnStickerRecommendListener(OnStickerRecommendListener listener) {
        mListener = listener;
    }
}
