/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

/**
 * 语音输入的声波：一排圆角竖条，中间高两边低，高度随音量平滑起落；录音结束、等待识别结果时换成三个依次跳动的圆点
 */
public class VoiceWaveView extends View {
    // 竖条升高、回落的时间常数，回落慢一些更自然
    private static final float RISE_TIME_MS = 50;
    private static final float FALL_TIME_MS = 140;
    // 声波和等待动画之间切换的时间常数
    private static final float LOADING_SWITCH_TIME_MS = 100;
    private static final int LOADING_DOT_COUNT = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final float barWidth;
    private final float barGap;
    private final float minBarHeight;
    private final float dotRadius;
    private final float dotSpacing;
    private int barColor = Color.WHITE;

    private float level;
    private boolean loading;
    // 等待动画的显示程度，0 显示声波，1 显示等待动画
    private float loadingProgress;
    // 每个竖条当前和目标的高度比例，0~1
    private float[] heights = new float[0];
    private float[] targets = new float[0];
    private long lastFrameTime;

    public VoiceWaveView(Context context) {
        this(context, null);
    }

    public VoiceWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        barWidth = 2 * density;
        barGap = 2.2f * density;
        minBarHeight = 3 * density;
        dotRadius = 2.6f * density;
        dotSpacing = 9 * density;
    }

    public void setBarColor(int color) {
        barColor = color;
        invalidate();
    }

    /**
     * @param level 音量，0~1
     */
    public void setLevel(float level) {
        this.level = Math.max(0, Math.min(1, level));
        updateTargets();
    }

    /**
     * @param loading 是否显示等待识别结果的动画
     */
    public void setLoading(boolean loading) {
        if (this.loading != loading) {
            this.loading = loading;
            invalidate();
        }
    }

    private void updateTargets() {
        int count = targets.length;
        for (int i = 0; i < count; i++) {
            float x = count == 1 ? 0 : (float) i / (count - 1) * 2 - 1;
            float envelope = 0.3f + 0.7f * (float) Math.exp(-x * x * 2.5f);
            float jitter = 0.5f + 0.5f * random.nextFloat();
            // 不说话时也保留一点起伏
            float idle = 0.06f + 0.1f * random.nextFloat() * envelope;
            targets[i] = Math.min(1, idle + level * envelope * jitter * 1.2f);
        }
        invalidate();
    }

    private void ensureBars() {
        int count = Math.max(1, (int) ((getWidth() + barGap) / (barWidth + barGap)));
        if (count == heights.length) {
            return;
        }
        // 宽度变化时按比例重采样，形变过程中竖条高度不会突变
        float[] newHeights = new float[count];
        for (int i = 0; i < count; i++) {
            newHeights[i] = heights.length == 0 ? 0 : heights[Math.min(heights.length - 1, i * heights.length / count)];
        }
        heights = newHeights;
        targets = new float[count];
        updateTargets();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        ensureBars();
        long now = SystemClock.uptimeMillis();
        float dt = lastFrameTime == 0 ? 16 : Math.min(64, now - lastFrameTime);
        lastFrameTime = now;
        loadingProgress += ((loading ? 1 : 0) - loadingProgress) * (1 - (float) Math.exp(-dt / LOADING_SWITCH_TIME_MS));

        int colorAlpha = Color.alpha(barColor);
        paint.setColor(barColor);
        int count = heights.length;
        float totalWidth = count * barWidth + (count - 1) * barGap;
        float left = (getWidth() - totalWidth) / 2;
        float centerY = getHeight() / 2f;
        float range = Math.max(0, getHeight() - minBarHeight);
        // 等待识别结果时竖条落下并淡出
        float barAlpha = 1 - loadingProgress;
        paint.setAlpha(Math.round(colorAlpha * barAlpha));
        for (int i = 0; i < count; i++) {
            float target = loading ? 0 : targets[i];
            float timeConstant = target > heights[i] ? RISE_TIME_MS : FALL_TIME_MS;
            heights[i] += (target - heights[i]) * (1 - (float) Math.exp(-dt / timeConstant));
            if (barAlpha > 0.01f) {
                float h = minBarHeight + range * heights[i];
                canvas.drawRoundRect(left, centerY - h / 2, left + barWidth, centerY + h / 2, barWidth / 2, barWidth / 2, paint);
            }
            left += barWidth + barGap;
        }
        if (loadingProgress > 0.01f) {
            // 三个圆点从左到右依次变大变亮
            double phase = now / 1000.0 * Math.PI * 2 * 1.2;
            float centerX = getWidth() / 2f;
            for (int i = 0; i < LOADING_DOT_COUNT; i++) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(phase - i * 0.9);
                float radius = dotRadius * (0.7f + 0.3f * pulse) * (0.5f + 0.5f * loadingProgress);
                paint.setAlpha(Math.round(colorAlpha * loadingProgress * (0.4f + 0.6f * pulse)));
                canvas.drawCircle(centerX + (i - (LOADING_DOT_COUNT - 1) / 2f) * dotSpacing, centerY, radius, paint);
            }
        }
        if (isShown()) {
            postInvalidateOnAnimation();
        } else {
            lastFrameTime = 0;
        }
    }
}
