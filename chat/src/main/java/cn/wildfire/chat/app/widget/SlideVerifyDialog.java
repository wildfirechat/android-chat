/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.callback.SlideVerifyCallback;
import cn.wildfire.chat.app.model.SlideVerifyInfo;
import cn.wildfirechat.chat.R;

/**
 * 滑动验证码对话框
 * <p>
 * 职责：负责滑动验证码的 UI 展示和交互逻辑
 * 网络请求统一由 {@link AppService} 处理
 * <p>
 */
public class SlideVerifyDialog extends Dialog {

    // ==================== UI 组件 ====================
    private ImageView backgroundImageView;
    private ImageView sliderImageView;
    private View sliderButton;
    private TextView sliderHintText;
    private FrameLayout sliderTrackContainer;
    private View containerView;

    // ==================== 状态变量 ====================
    private String currentToken;
    private double sliderY;
    private float startX;
    private boolean isProcessing = false;
    private float imageScaleRatio = 1.0f; // 图片缩放比例

    // ==================== 常量 ====================
    private static final float MIN_SLIDE_DISTANCE = 10f;
    private static final int ANIMATION_DURATION = 200;
    private static final int SUCCESS_DELAY_MS = 500;
    private static final int FAILURE_DELAY_MS = 1000;
    private static final int COLOR_SUCCESS = 0xFF4CAF50;
    private static final int COLOR_FAILURE = 0xFFF44336;
    private static final int COLOR_NORMAL = 0xFF999999;
    private static final int ORIGINAL_IMAGE_WIDTH = 300;

    // ==================== 回调接口 ====================
    private final OnVerifySuccessListener verifyListener;

    /**
     * 验证结果回调接口
     */
    public interface OnVerifySuccessListener {
        /**
         * 验证成功
         *
         * @param token 验证令牌
         */
        void onVerifySuccess(@NonNull String token);

        /**
         * 验证失败（滑动位置不正确），不关闭窗口
         */
        void onVerifyFailed();

        /**
         * 加载验证码失败，需要关闭窗口
         */
        void onLoadFailed();
    }

    public SlideVerifyDialog(@NonNull Context context, @Nullable OnVerifySuccessListener listener) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        this.verifyListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWindowParams();
        setContentView(R.layout.slide_verify_dialog);
        initViews();
        setupClickListeners();
        setupSlider();
        loadVerifyCode();
    }

    /**
     * 初始化窗口参数
     */
    private void initWindowParams() {
        if (getWindow() == null) {
            return;
        }
        
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.CENTER;
        
        // 设置半透明背景（黑色 50% 透明度）
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#80000000")));
        
        // 设置状态栏颜色为半透明黑色
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        params.dimAmount = 0.5f;

        getWindow().setAttributes(params);
        setCanceledOnTouchOutside(true);
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        backgroundImageView = findViewById(R.id.backgroundImageView);
        sliderImageView = findViewById(R.id.sliderImageView);
        sliderButton = findViewById(R.id.sliderButton);
        sliderHintText = findViewById(R.id.sliderHintText);
        sliderTrackContainer = findViewById(R.id.sliderTrackContainer);
        containerView = findViewById(R.id.containerView);
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        // 根布局点击监听（点击外部关闭）
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> onOutsideClick());

        // 容器点击监听（阻止事件传递）
        if (containerView != null) {
            containerView.setOnClickListener(v -> {
                // 点击容器内部，不做任何事，阻止事件传递到根布局
            });
        }
    }

    /**
     * 处理外部点击事件
     */
    private void onOutsideClick() {
        dismiss();
        if (verifyListener != null) {
            verifyListener.onLoadFailed();
        }
        Toast.makeText(getContext(), "已取消", Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置滑块拖动监听
     */
    private void setupSlider() {
        sliderButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        return handleActionDown(event);

                    case MotionEvent.ACTION_MOVE:
                        return handleActionMove(event);

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        return handleActionUp();

                    default:
                        return false;
                }
            }
        });
    }

    /**
     * 处理手指按下事件
     */
    private boolean handleActionDown(MotionEvent event) {
        if (isProcessing) {
            return false;
        }
        startX = event.getRawX() - sliderButton.getX();
        return true;
    }

    /**
     * 处理手指移动事件
     */
    private boolean handleActionMove(MotionEvent event) {
        float newX = event.getRawX() - startX;
        float maxDistance = sliderTrackContainer.getWidth() - sliderButton.getWidth();
        newX = Math.max(0, Math.min(newX, maxDistance));

        sliderButton.setX(newX);
        sliderImageView.setX(newX);
        return true;
    }

    /**
     * 处理手指抬起事件
     */
    private boolean handleActionUp() {
        float finalX = sliderButton.getX();

        if (finalX >= MIN_SLIDE_DISTANCE) {
            performVerify(finalX);
        } else {
            // 滑动距离不足，复位（使用动画）
            resetSliderPosition(true);
        }
        return true;
    }

    /**
     * 执行验证
     *
     * @param slideX 滑动距离
     */
    private void performVerify(float slideX) {
        if (isProcessing || currentToken == null) {
            return;
        }
        isProcessing = true;

        int originalX = calculateOriginalX(slideX);

        AppService.Instance().verifySlidePosition(currentToken, originalX, new SlideVerifyCallback() {
            @Override
            public void onLoadSuccess(@NonNull SlideVerifyInfo verifyInfo) {
                // 不会在此回调中被调用
            }

            @Override
            public void onLoadFailure(int errorCode, @NonNull String errorMsg) {
                // 不会在此回调中被调用
            }

            @Override
            public void onVerifySuccess(@NonNull String token) {
                handleVerifySuccess();
            }

            @Override
            public void onVerifyFailure(int errorCode, @NonNull String errorMsg) {
                handleVerifyFailure();
            }
        });
    }

    /**
     * 计算原始 X 坐标（考虑缩放）
     */
    private int calculateOriginalX(float slideX) {
        // 使用保存的缩放比例，避免动态获取视图宽度不准确
        return (int) (slideX / imageScaleRatio);
    }

    /**
     * 处理验证成功
     */
    private void handleVerifySuccess() {
        isProcessing = false;

        // 更新 UI 状态
        GradientDrawable drawable = (GradientDrawable) sliderButton.getBackground();
        drawable.setColor(COLOR_SUCCESS);
        sliderHintText.setText("验证成功");
        sliderHintText.setTextColor(COLOR_SUCCESS);

        // 延迟关闭
        sliderButton.postDelayed(() -> {
            if (verifyListener != null) {
                verifyListener.onVerifySuccess(currentToken);
            }
            dismiss();
        }, SUCCESS_DELAY_MS);
    }

    /**
     * 处理验证失败
     */
    private void handleVerifyFailure() {
        isProcessing = false;

        // 重置滑块位置（使用动画）
        resetSliderPosition(true);

        // 更新提示文字
        sliderHintText.setText("验证失败，请重试");
        sliderHintText.setTextColor(COLOR_FAILURE);

        if (verifyListener != null) {
            verifyListener.onVerifyFailed();
        }

        // 延迟刷新验证码
        sliderButton.postDelayed(() -> {
            sliderHintText.setText("向右滑动完成验证");
            sliderHintText.setTextColor(COLOR_NORMAL);
            loadVerifyCode();
        }, FAILURE_DELAY_MS);
    }

    /**
     * 重置滑块位置
     *
     * @param animate 是否使用动画
     */
    private void resetSliderPosition(boolean animate) {
        if (animate) {
            sliderButton.animate()
                .x(0)
                .setDuration(ANIMATION_DURATION)
                .start();
            sliderImageView.animate()
                .x(0)
                .setDuration(ANIMATION_DURATION)
                .start();
        } else {
            // 直接设置位置，无动画
            sliderButton.setX(0);
            sliderImageView.setX(0);
        }
    }

    /**
     * 加载验证码
     */
    private void loadVerifyCode() {
        if (isProcessing) {
            return;
        }
        isProcessing = true;

        AppService.Instance().loadSlideVerifyCode(new SlideVerifyCallback() {
            @Override
            public void onLoadSuccess(@NonNull SlideVerifyInfo verifyInfo) {
                isProcessing = false;
                handleVerifyCodeLoaded(verifyInfo);
            }

            @Override
            public void onLoadFailure(int errorCode, @NonNull String errorMsg) {
                isProcessing = false;
                handleVerifyCodeLoadFailure(errorMsg);
            }

            @Override
            public void onVerifySuccess(@NonNull String token) {
                // 不会在此回调中被调用
            }

            @Override
            public void onVerifyFailure(int errorCode, @NonNull String errorMsg) {
                // 不会在此回调中被调用
            }
        });
    }

    /**
     * 处理验证码加载成功
     */
    private void handleVerifyCodeLoaded(@NonNull SlideVerifyInfo verifyInfo) {
        currentToken = verifyInfo.getToken();
        sliderY = verifyInfo.getSliderY();

        // 设置图片
        backgroundImageView.setImageBitmap(verifyInfo.getBackgroundBitmap());
        sliderImageView.setImageBitmap(verifyInfo.getSliderBitmap());

        // 延迟到布局完成后更新布局参数，确保能获取正确的视图尺寸
        backgroundImageView.post(() -> {
            resetSliderPosition(false);
            updateSliderLayout(verifyInfo);
        });
    }

    /**
     * 更新滑块布局参数
     */
    private void updateSliderLayout(@NonNull SlideVerifyInfo verifyInfo) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) sliderImageView.getLayoutParams();

        // 计算并保存图片缩放比例，用于后续坐标转换
        int displayWidth = backgroundImageView.getWidth();
        int originalWidth = verifyInfo.getOriginalWidth();
        if (displayWidth > 0 && originalWidth > 0) {
            imageScaleRatio = (float) displayWidth / originalWidth;
        }
        
        float scaleY = (float) backgroundImageView.getHeight() / verifyInfo.getOriginalHeight();
        params.topMargin = (int) (sliderY * scaleY);

        params.width = (int) (verifyInfo.getSliderBitmap().getWidth() * imageScaleRatio);

        sliderImageView.setLayoutParams(params);
    }

    /**
     * 处理验证码加载失败
     */
    private void handleVerifyCodeLoadFailure(@NonNull String errorMsg) {
        Toast.makeText(getContext(), "加载验证码失败：" + errorMsg, Toast.LENGTH_SHORT).show();
        if (verifyListener != null) {
            verifyListener.onLoadFailed();
        }
        dismiss();
    }

    @Override
    public void dismiss() {
        if (isProcessing) {
            return;
        }
        super.dismiss();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 清理资源
        currentToken = null;
    }
}
