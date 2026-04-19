/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.google.android.material.imageview.ShapeableImageView;

import cn.wildfire.chat.kit.R;

/**
 * 支持圆角和 WebP 动画的 ImageView，并保留进度百分比显示功能。
 */
public class BubbleImageView extends ShapeableImageView{

    private int mAngle = dp2px(10);
    private int percent = 0;
    private boolean mShowText = false;
    private boolean mShowShadow = false;

    private Paint mPaint;
    private Paint mMaskPaint;

    public BubbleImageView(Context context) {
        this(context, null);
    }

    public BubbleImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setColor(Color.BLACK);
        mMaskPaint.setStyle(Paint.Style.FILL);
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    private void initView(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BubbleImageView);
            mAngle = (int) a.getDimension(R.styleable.BubbleImageView_bubble_angle, mAngle);
            mShowText = a.getBoolean(R.styleable.BubbleImageView_bubble_showText, mShowText);
            mShowShadow = a.getBoolean(R.styleable.BubbleImageView_bubble_showShadow, mShowShadow);
            a.recycle();
        }
    }

    /**
     * 是否显示阴影
     */
    public void showShadow(boolean showShadow) {
        this.mShowShadow = showShadow;
        postInvalidate();
    }

    /**
     * 设置进度的百分比
     */
    public void setPercent(int percent) {
        this.percent = percent;
        postInvalidate();
    }

    /**
     * 设置进度文字是否显示
     */
    public void setProgressVisible(boolean show) {
        this.mShowText = show;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() == null) {
            return;
        }

        RectF rect = new RectF(0, 0, getWidth(), getHeight());

        // 使用离屏缓冲绘制，以支持带有动画的Drawable（如WebP, GIF）以及抗锯齿遮罩
        int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null, Canvas.ALL_SAVE_FLAG);

        // 1. 绘制实际的图片（保留了动画能力）
        super.onDraw(canvas);

        // 2. 绘制圆角遮罩进行裁剪
        canvas.drawRoundRect(rect, mAngle, mAngle, mMaskPaint);

        canvas.restoreToCount(saveCount);

        // 3. 绘制进度阴影和文字
        drawProgress(canvas);
    }

    private void drawProgress(Canvas canvas) {
        if (!mShowShadow && !mShowText) {
            return;
        }

        mPaint.setStyle(Paint.Style.FILL);

        if (mShowShadow && percent < 100) {
            mPaint.setColor(Color.parseColor("#70000000"));
            float shadowHeight = getHeight() * (1 - percent / 100f);
            RectF shadowRectF = new RectF(0, 0, getWidth() - getPaddingRight(), shadowHeight);
            canvas.drawRoundRect(shadowRectF, mAngle, mAngle, mPaint);
        }

        if (mShowText && percent < 100) {
            mPaint.setTextSize(dp2px(14));
            mPaint.setColor(Color.WHITE);
            mPaint.setFakeBoldText(true);

            String text = percent + "%";
            Rect textBounds = new Rect();
            mPaint.getTextBounds(text, 0, text.length(), textBounds);

            float x = (getWidth() - textBounds.width()) / 2f;
            float y = (getHeight() + textBounds.height()) / 2f;
            canvas.drawText(text, x, y, mPaint);
        }
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }
}
