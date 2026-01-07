/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget.selecttext;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.ColorInt;

/**
 * 继承ImageSpan，绘制图片背景
 * https://developer.android.google.cn/reference/android/text/style/DynamicDrawableSpan
 *
 * Create by gnmmdk
 */
public class SelectImageSpan extends ImageSpan {
    @ColorInt
    public int bgColor;

    public SelectImageSpan(Drawable drawable, @ColorInt int bgColor, int verticalAlignment) {
        super(drawable, verticalAlignment);
        this.bgColor = bgColor;
    }

    /**
     * 重写 draw 方法
     * 绘制背景
     */
    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        Drawable d = getDrawable();
        canvas.save();

        // 修改canvas paint颜色实现
        paint.setColor(bgColor);
        canvas.drawRect(x, top, x + d.getBounds().right, bottom, paint);

        // 处理图片绘制位置
        int transY = bottom - d.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
        } else if (mVerticalAlignment == ALIGN_CENTER) {
            transY = top + (bottom - top) / 2 - d.getBounds().height() / 2;
        }
        canvas.translate(x, transY);
        d.draw(canvas);
        canvas.restore();
    }
}
