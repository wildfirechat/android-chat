/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.app.R;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.model.Conversation;

public class SlideVerifyDialog extends Dialog {

    private ImageView backgroundImageView;
    private ImageView sliderImageView;
    private View sliderButton;
    private TextView sliderHintText;
    private FrameLayout sliderTrackContainer;

    private String token;
    private int sliderY;
    private float startX;
    private boolean isVerifying = false;

    private OnVerifySuccessListener listener;

    public interface OnVerifySuccessListener {
        void onVerifySuccess(String token);
        void onVerifyFailed(); // 验证失败（滑动位置不对），不关闭窗口
        void onLoadFailed(); // 加载验证码失败，需要关闭窗口
    }

    public SlideVerifyDialog(@NonNull Context context, OnVerifySuccessListener listener) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        this.listener = listener;
        init();
    }

    private void init() {
        setContentView(R.layout.slide_verify_dialog);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.CENTER;
        getWindow().setAttributes(params);

        // 设置对话框外部点击取消
        setCanceledOnTouchOutside(true);

        // 获取根布局并设置点击监听
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> {
            // 点击外部区域，取消验证
            dismiss();
            if (listener != null) {
                listener.onLoadFailed();
            }
            Toast.makeText(getContext(), "已取消", Toast.LENGTH_SHORT).show();
        });

        backgroundImageView = findViewById(R.id.backgroundImageView);
        sliderImageView = findViewById(R.id.sliderImageView);
        sliderButton = findViewById(R.id.sliderButton);
        sliderHintText = findViewById(R.id.sliderHintText);
        sliderTrackContainer = findViewById(R.id.sliderTrackContainer);

        // 设置容器点击事件，不传递给根布局
        View containerView = findViewById(R.id.containerView);
        if (containerView != null) {
            containerView.setOnClickListener(v -> {
                // 点击容器内部，不做任何事
            });
        }

        setupSlider();
        loadVerifyCode();
    }

    private void setupSlider() {
        sliderButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX() - sliderButton.getX();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() - startX;
                        float maxDistance = sliderTrackContainer.getWidth() - sliderButton.getWidth();
                        newX = Math.max(0, Math.min(newX, maxDistance));
                        sliderButton.setX(newX);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float finalX = sliderButton.getX();
                        float maxDistance = sliderTrackContainer.getWidth() - sliderButton.getWidth();

                        // 只要滑动超过 10px 就验证
                        if (finalX >= 10) {
                            // Verify
                            verifySlidePosition((int) finalX);
                        } else {
                            // Reset position
                            sliderButton.animate().x(0).setDuration(200).start();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void loadVerifyCode() {
        if (isVerifying) return;
        isVerifying = true;

        String url = cn.wildfire.chat.app.AppService.APP_SERVER_ADDRESS + "/slide_verify/generate";
        Map<String, Object> params = new HashMap<>();

        OKHttpHelper.post(url, params, new SimpleCallback<Map<String, Object>>() {
            @Override
            public void onUiSuccess(Map<String, Object> result) {
                isVerifying = false;

                // 检查返回数据是否完整
                if (result == null || result.get("token") == null || result.get("backgroundImage") == null || result.get("sliderImage") == null) {
                    Toast.makeText(getContext(), "加载验证码失败，数据不完整", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onLoadFailed();
                    }
                    dismiss();
                    return;
                }

                token = (String) result.get("token");
                String backgroundImageStr = (String) result.get("backgroundImage");
                String sliderImageStr = (String) result.get("sliderImage");
                sliderY = (int) result.get("y");

                // Decode base64 images
                try {
                    // 去除 data URI 前缀，提取纯 base64 数据
                    if (backgroundImageStr.contains(",")) {
                        backgroundImageStr = backgroundImageStr.substring(backgroundImageStr.indexOf(",") + 1);
                    }
                    if (sliderImageStr.contains(",")) {
                        sliderImageStr = sliderImageStr.substring(sliderImageStr.indexOf(",") + 1);
                    }

                    byte[] backgroundBytes = Base64.decode(backgroundImageStr, Base64.DEFAULT);
                    Bitmap backgroundBitmap = BitmapFactory.decodeByteArray(backgroundBytes, 0, backgroundBytes.length);
                    backgroundImageView.setImageBitmap(backgroundBitmap);

                    byte[] sliderBytes = Base64.decode(sliderImageStr, Base64.DEFAULT);
                    Bitmap sliderBitmap = BitmapFactory.decodeByteArray(sliderBytes, 0, sliderBytes.length);
                    sliderImageView.setImageBitmap(sliderBitmap);

                    // Position slider image based on y coordinate
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) sliderImageView.getLayoutParams();
                    float scaleY = (float) backgroundImageView.getHeight() / 150f;
                    params.topMargin = (int) (sliderY * scaleY);
                    sliderImageView.setLayoutParams(params);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "加载验证码失败", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onLoadFailed();
                    }
                    dismiss();
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                isVerifying = false;
                Toast.makeText(getContext(), "加载验证码失败：" + msg, Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onLoadFailed();
                }
                dismiss();
            }
        });
    }

    private void verifySlidePosition(int x) {
        if (isVerifying) return;
        isVerifying = true;

        // Calculate original x position based on image scale
        float scaleX = (float) backgroundImageView.getWidth() / 300f;
        int originalX = (int) (x / scaleX);

        String url = cn.wildfire.chat.app.AppService.APP_SERVER_ADDRESS + "/slide_verify/verify";
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("x", originalX);

        OKHttpHelper.post(url, params, new SimpleCallback<Object>() {
            @Override
            public void onUiSuccess(Object result) {
                isVerifying = false;
                // Change button color to green
                GradientDrawable drawable = (GradientDrawable) sliderButton.getBackground();
                drawable.setColor(0xFF4CAF50);
                sliderHintText.setText("验证成功");
                sliderHintText.setTextColor(0xFF4CAF50);

                // Dismiss after delay
                sliderButton.postDelayed(() -> {
                    if (listener != null) {
                        listener.onVerifySuccess(token);
                    }
                    dismiss();
                }, 500);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                isVerifying = false;
                // Reset slider
                sliderButton.animate().x(0).setDuration(200).start();
                sliderHintText.setText("验证失败，请重试");
                sliderHintText.setTextColor(0xFFF44336);

                // Refresh after delay
                sliderButton.postDelayed(() -> {
                    sliderHintText.setText("向右滑动完成验证");
                    sliderHintText.setTextColor(0xFF999999);
                    loadVerifyCode();
                }, 1000);
            }
        });
    }

    @Override
    public void dismiss() {
        if (isVerifying) {
            return;
        }
        super.dismiss();
    }
}
