/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package me.aurelion.x.ui.view.watermark;

import android.graphics.Color;
import android.graphics.Paint;

/**
 * @author Leon (wshk729@163.com)
 * @date 2018/8/24
 * <p>
 */
public class WaterMarkInfo {

    private int mDegrees;
    private int mTextColor;
    private int mTextSize;
    private boolean mTextBold;
    private int mDx;
    private int mDy;
    private Paint.Align mAlign;

    private WaterMarkInfo(int degrees, int textColor, int textSize, boolean textBold, int dx, int dy, Paint.Align align) {
        mDegrees = degrees;
        mTextColor = textColor;
        mTextSize = textSize;
        mTextBold = textBold;
        mDx = dx;
        mDy = dy;
        mAlign = align;
    }

    public int getDegrees() {
        return mDegrees;
    }

    public int getTextColor() {
        return mTextColor;
    }

    public int getTextSize() {
        return mTextSize;
    }

    public int getDx() {
        return mDx;
    }

    public int getDy() {
        return mDy;
    }

    public Paint.Align getAlign() {
        return mAlign;
    }

    public int getAlignInt() {
        switch (mAlign) {
            case LEFT:
                return 0;
            case RIGHT:
                return 2;
            default:
                return 1;
        }
    }

    public boolean isTextBold() {
        return mTextBold;
    }

    void setDegrees(int degrees) {
        mDegrees = degrees;
    }

    void setTextColor(int textColor) {
        mTextColor = textColor;
    }

    void setTextSize(int textSize) {
        mTextSize = textSize;
    }

    void setTextBold(boolean textBold) {
        mTextBold = textBold;
    }

    void setDx(int dx) {
        mDx = dx;
    }

    void setDy(int dy) {
        mDy = dy;
    }

    void setAlign(Paint.Align align) {
        this.mAlign = align;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private int mDegrees;
        private int mTextColor;
        private int mTextSize;
        private boolean mTextBold;
        private int mDx;
        private int mDy;
        private Paint.Align mAlign;

        private Builder() {
            mDegrees = -30;
            mTextColor = Color.parseColor("#33000000");
            mTextSize = 42;
            mTextBold = false;
            mDx = 100;
            mDy = 240;
            mAlign = Paint.Align.CENTER;
        }

        /**
         * 设置水印文字倾斜度
         *
         * @param degrees 文字倾斜度(默认:-30)
         * @return Builder
         */
        public Builder setDegrees(int degrees) {
            mDegrees = degrees;
            return this;
        }

        /**
         * 设置水印文字颜色
         *
         * @param textColor 文字颜色(默认:#33000000)
         * @return Builder
         */
        public Builder setTextColor(int textColor) {
            mTextColor = textColor;
            return this;
        }

        /**
         * 设置水印文字大小（单位：px）
         *
         * @param textSize 文字大小(默认:42px)
         * @return Builder
         */
        public Builder setTextSize(int textSize) {
            mTextSize = textSize;
            return this;
        }

        /**
         * 设置水印文字是否加粗
         *
         * @param textBold 文字加粗(默认:false)
         * @return Builder
         */
        public Builder setTextBold(boolean textBold) {
            mTextBold = textBold;
            return this;
        }

        /**
         * 设置水印文字X轴间距（单位：px）
         *
         * @param dx 文字X轴间距(默认:100px)
         * @return Builder
         */
        public Builder setDx(int dx) {
            mDx = dx;
            return this;
        }

        /**
         * 设置水印文字Y轴间距（单位：px）
         *
         * @param dy 文字Y轴间距(默认:240px)
         * @return Builder
         */
        public Builder setDy(int dy) {
            mDy = dy;
            return this;
        }

        /**
         * 设置水印文字对齐方式
         *
         * @param align 对齐方式(默认:Center)
         * @return Builder
         */
        public Builder setAlign(Paint.Align align) {
            mAlign = align;
            return this;
        }

        /**
         * 生成水印全局配置信息
         *
         * @return 配置信息
         */
        public WaterMarkInfo generate() {
            return new WaterMarkInfo(mDegrees, mTextColor, mTextSize, mTextBold, mDx, mDy, mAlign);
        }
    }

}