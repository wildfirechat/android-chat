/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 滑动验证码信息模型类
 * 封装验证码相关的所有数据，包括图片、位置信息和令牌
 */
public class SlideVerifyInfo {

    /** 验证令牌 */
    private final String token;
    
    /** 背景图片 Bitmap */
    private final Bitmap backgroundBitmap;
    
    /** 滑块图片 Bitmap */
    private final Bitmap sliderBitmap;
    
    /** 滑块 Y 轴位置（原始坐标） */
    private final double sliderY;
    
    /** 原始背景图片宽度 */
    private final int originalWidth;
    
    /** 原始背景图片高度 */
    private final int originalHeight;

    private SlideVerifyInfo(Builder builder) {
        this.token = builder.token;
        this.backgroundBitmap = builder.backgroundBitmap;
        this.sliderBitmap = builder.sliderBitmap;
        this.sliderY = builder.sliderY;
        this.originalWidth = builder.originalWidth;
        this.originalHeight = builder.originalHeight;
    }

    @NonNull
    public String getToken() {
        return token;
    }

    @NonNull
    public Bitmap getBackgroundBitmap() {
        return backgroundBitmap;
    }

    @NonNull
    public Bitmap getSliderBitmap() {
        return sliderBitmap;
    }

    public double getSliderY() {
        return sliderY;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    /**
     * 释放 Bitmap 资源，防止内存泄漏
     */
    public void recycle() {
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
        }
        if (sliderBitmap != null && !sliderBitmap.isRecycled()) {
            sliderBitmap.recycle();
        }
    }

    /**
     * Builder 模式，用于构建 SlideVerifyInfo 实例
     */
    public static class Builder {
        private String token;
        private Bitmap backgroundBitmap;
        private Bitmap sliderBitmap;
        private double sliderY;
        private int originalWidth;
        private int originalHeight;

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setBackgroundBitmap(Bitmap backgroundBitmap) {
            this.backgroundBitmap = backgroundBitmap;
            this.originalWidth = backgroundBitmap != null ? backgroundBitmap.getWidth() : 0;
            this.originalHeight = backgroundBitmap != null ? backgroundBitmap.getHeight() : 0;
            return this;
        }

        public Builder setSliderBitmap(Bitmap sliderBitmap) {
            this.sliderBitmap = sliderBitmap;
            return this;
        }

        public Builder setSliderY(double sliderY) {
            this.sliderY = sliderY;
            return this;
        }

        /**
         * 从 Base64 字符串解码并设置背景图片
         */
        public Builder setBackgroundImageFromBase64(@Nullable String base64String) {
            this.backgroundBitmap = decodeBase64ToBitmap(base64String);
            this.originalWidth = backgroundBitmap != null ? backgroundBitmap.getWidth() : 0;
            this.originalHeight = backgroundBitmap != null ? backgroundBitmap.getHeight() : 0;
            return this;
        }

        /**
         * 从 Base64 字符串解码并设置滑块图片
         */
        public Builder setSliderImageFromBase64(@Nullable String base64String) {
            this.sliderBitmap = decodeBase64ToBitmap(base64String);
            return this;
        }

        public SlideVerifyInfo build() {
            validate();
            return new SlideVerifyInfo(this);
        }

        private void validate() {
            if (token == null || token.isEmpty()) {
                throw new IllegalStateException("Token cannot be null or empty");
            }
            if (backgroundBitmap == null) {
                throw new IllegalStateException("Background bitmap cannot be null");
            }
            if (sliderBitmap == null) {
                throw new IllegalStateException("Slider bitmap cannot be null");
            }
        }

        @Nullable
        private static Bitmap decodeBase64ToBitmap(@Nullable String base64String) {
            if (base64String == null || base64String.isEmpty()) {
                return null;
            }

            try {
                // 去除 data URI 前缀，提取纯 base64 数据
                String pureBase64 = extractBase64Data(base64String);
                byte[] decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @NonNull
        private static String extractBase64Data(@NonNull String base64String) {
            int commaIndex = base64String.indexOf(',');
            if (commaIndex >= 0 && commaIndex < base64String.length() - 1) {
                return base64String.substring(commaIndex + 1);
            }
            return base64String;
        }
    }
}
