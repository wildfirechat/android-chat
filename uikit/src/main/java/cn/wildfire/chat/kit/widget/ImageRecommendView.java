/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatTextView;

import com.bumptech.glide.Glide;

import java.io.File;

/**
 * 图片推荐视图
 * 当用户截图或拍照后，显示图片缩略图推荐
 */
public class ImageRecommendView extends FrameLayout {
    private androidx.appcompat.widget.AppCompatTextView mHintTextView;
    private ImageView mThumbnailImageView;
    private String mCurrentImagePath;
    private Handler mAutoHideHandler;
    private Runnable mAutoHideRunnable;
    private static final int AUTO_HIDE_DELAY = 30000;  // 30秒自动隐藏

    public ImageRecommendView(Context context) {
        super(context);
        initView(context);
    }

    private void initView(Context context) {
        setPadding(dpToPx(context, 12), dpToPx(context, 12), dpToPx(context, 12), dpToPx(context, 12));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.END);

        // 提示文字
        mHintTextView = new AppCompatTextView(context);
        mHintTextView.setText("你可能想发送");
        mHintTextView.setTextSize(14);
        mHintTextView.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.gravity = Gravity.CENTER_HORIZONTAL;
        mHintTextView.setLayoutParams(hintParams);

        // 缩略图
        mThumbnailImageView = new ImageView(context);
        mThumbnailImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(
            dpToPx(context, 100), dpToPx(context, 100)
        );
        thumbParams.topMargin = dpToPx(context, 8);
        mThumbnailImageView.setLayoutParams(thumbParams);
        mThumbnailImageView.setOnClickListener(v -> onThumbnailClicked());

        layout.addView(mHintTextView);
        layout.addView(mThumbnailImageView);

        addView(layout);

        setVisibility(GONE);

        // 初始化自动隐藏 Handler
        mAutoHideHandler = new Handler(Looper.getMainLooper());
        mAutoHideRunnable = () -> hide();
    }

    /**
     * 显示图片推荐
     * @param imagePath 图片路径
     */
    public void showImage(String imagePath) {
        mCurrentImagePath = imagePath;

        // 加载缩略图
        Glide.with(getContext())
            .load(new File(imagePath))
            .centerCrop()
            .into(mThumbnailImageView);

        setVisibility(VISIBLE);

        // 启动自动隐藏倒计时
        startAutoHideTimer();
    }

    /**
     * 隐藏推荐视图
     */
    public void hide() {
        setVisibility(GONE);
        mCurrentImagePath = null;
        stopAutoHideTimer();

        // 通知外部 dismiss PopupWindow
        if (mListener != null) {
            mListener.onDismiss();
        }
    }

    /**
     * 启动自动隐藏倒计时
     */
    private void startAutoHideTimer() {
        stopAutoHideTimer();
        mAutoHideHandler.postDelayed(mAutoHideRunnable, AUTO_HIDE_DELAY);
    }

    /**
     * 停止自动隐藏倒计时
     */
    private void stopAutoHideTimer() {
        mAutoHideHandler.removeCallbacks(mAutoHideRunnable);
    }

    /**
     * 处理缩略图点击
     */
    private void onThumbnailClicked() {
        if (mCurrentImagePath != null && mListener != null) {
            mListener.onImageRecommendSelected(mCurrentImagePath);
        }
        hide();
    }

    private int dpToPx(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * 图片推荐监听器
     */
    public interface OnImageRecommendListener {
        void onImageRecommendSelected(String imagePath);
        void onDismiss();
    }

    private OnImageRecommendListener mListener;

    public void setOnImageRecommendListener(OnImageRecommendListener listener) {
        mListener = listener;
    }
}
