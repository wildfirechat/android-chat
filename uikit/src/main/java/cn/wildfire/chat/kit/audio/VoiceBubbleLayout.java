/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 语音输入气泡：圆角矩形，底部有一个指向当前手势目标的小尖角
 * <p>
 * 尖角画在底部 {@link #getTailHeight()} 的范围内，paddingBottom 需要包含这部分高度
 */
public class VoiceBubbleLayout extends FrameLayout {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tailPath = new Path();
    private final RectF bodyRect = new RectF();
    private final float density;
    private final float radius;
    private final float tailWidth;
    private final float tailHeight;
    // 尖角中心相对于气泡左边的位置，小于 0 时居中
    private float tailX = -1;

    public VoiceBubbleLayout(@NonNull Context context) {
        this(context, null);
    }

    public VoiceBubbleLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        density = getResources().getDisplayMetrics().density;
        radius = 18 * density;
        tailWidth = 18 * density;
        tailHeight = 8 * density;
        paint.setColor(0xFF95EC69);
    }

    public float getTailHeight() {
        return tailHeight;
    }

    public int getBubbleColor() {
        return paint.getColor();
    }

    public void setBubbleColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void setTailX(float tailX) {
        this.tailX = tailX;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float bodyBottom = height - tailHeight;
        if (width <= 0 || bodyBottom <= 0) {
            return;
        }
        float r = Math.min(radius, Math.min(width, bodyBottom) / 2);
        bodyRect.set(0, 0, width, bodyBottom);
        canvas.drawRoundRect(bodyRect, r, r, paint);

        float half = tailWidth / 2;
        float x = tailX < 0 ? width / 2 : tailX;
        x = Math.max(Math.min(r + half, width / 2), Math.min(Math.max(width - r - half, width / 2), x));
        // 尖角向上多画 1px 与气泡主体重叠，避免抗锯齿留下接缝；尖端画成小圆角
        float tip = 1.5f * density;
        tailPath.reset();
        tailPath.moveTo(x - half, bodyBottom - 1);
        tailPath.lineTo(x - tip, height - density);
        tailPath.quadTo(x, height, x + tip, height - density);
        tailPath.lineTo(x + half, bodyBottom - 1);
        tailPath.close();
        canvas.drawPath(tailPath, paint);
    }
}
