/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wildfire.chat.kit.R;

/**
 * 按住说话时的底部操作区：最下方弧形的“松开 发送”区域，上方左右两条弧形的“取消”和“转文字”按钮
 * <p>
 * 通过 {@link #zoneAt(float, float)} 判断手指所在的目标，{@link #setZone(int)} 让对应的按钮平滑高亮
 */
public class VoiceRecordBottomView extends View {
    public static final int ZONE_SEND = 0;
    public static final int ZONE_CANCEL = 1;
    public static final int ZONE_TEXT = 2;

    // 配色参考微信深色模式，画在深灰背景上
    private static final int ARC_COLOR = 0xFF575757;
    private static final int ARC_SELECTED_TOP_COLOR = 0xFF636363;
    private static final int ARC_SELECTED_BOTTOM_COLOR = 0xFF808080;
    private static final int ARC_RIM_COLOR = 0xFF767676;
    private static final int ARC_LABEL_COLOR = 0xFFDDDDDD;
    private static final int PILL_COLOR = 0xFF575757;
    private static final int PILL_SELECTED_COLOR = 0xFF9A9A9A;
    private static final int PILL_LABEL_COLOR = 0xFFE6E6E6;
    private static final int SELECTED_LABEL_COLOR = 0xFF111111;
    private static final int HINT_COLOR = 0xFFD0D0D0;

    private final float density;
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcSelectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float pillLabelTextSize;
    private final float hintTextSize;
    private final Path textPath = new Path();
    private final RectF oval = new RectF();

    private final String voiceLabel;
    private final String sendLabel;
    private final String cancelLabel;
    private final String cancelHint;
    private final String textLabel;
    private final String textHint;

    private boolean speechToTextEnabled;
    private boolean hasStage;
    private float stageLeft;
    private float stageRight;
    private float stageWidth;
    private float centerX;
    // 底部弧形区域：圆心在底部中间下方的大圆
    private float arcTop;
    private float arcRadius;
    private float arcCenterY;
    private int shaderHeight;
    // 两条弧形按钮的中线在同一个更大的圆上
    private float pillThickness;
    private float pillGap;
    private float pillRadius;
    private float pillCenterY;
    // 按钮文字中心到中线的水平距离
    private float labelOffsetX;
    private float maxLabelWidth;

    private int zone = ZONE_SEND;
    // 各个目标的高亮程度，0~1，按 ZONE_* 索引
    private final float[] selection = {1, 0, 0};
    private ValueAnimator selectionAnimator;
    // 入场进度，0 完全隐藏，1 完全显示
    private float appear;
    private ValueAnimator appearAnimator;

    public VoiceRecordBottomView(Context context) {
        this(context, null);
    }

    public VoiceRecordBottomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        arcPaint.setColor(ARC_COLOR);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(2 * density);
        rimPaint.setColor(ARC_RIM_COLOR);
        pillPaint.setStyle(Paint.Style.STROKE);
        pillPaint.setStrokeCap(Paint.Cap.ROUND);

        arcLabelPaint.setTextAlign(Paint.Align.CENTER);
        arcLabelPaint.setTextSize(sp(18));
        pillLabelTextSize = sp(17);
        hintTextSize = sp(14);
        pillLabelPaint.setTextSize(pillLabelTextSize);
        hintPaint.setTextSize(hintTextSize);

        voiceLabel = context.getString(R.string.voice_input_voice);
        sendLabel = context.getString(R.string.voice_input_release_to_send);
        cancelLabel = context.getString(R.string.cancel);
        cancelHint = context.getString(R.string.voice_input_release_to_cancel);
        textLabel = context.getString(R.string.voice_input_slide_to_text);
        textHint = context.getString(R.string.voice_input_release_to_edit);
    }

    public void setSpeechToTextEnabled(boolean enabled) {
        speechToTextEnabled = enabled;
        invalidate();
    }

    /**
     * 设置操作区的位置
     *
     * @param left   左边界，本 View 坐标
     * @param right  右边界，本 View 坐标
     * @param arcTop 底部弧形区域最高点的 y 坐标，按住说话按钮需要在弧形区域内
     */
    public void setStage(float left, float right, float arcTop) {
        stageLeft = left;
        stageRight = right;
        stageWidth = right - left;
        centerX = left + stageWidth / 2;
        this.arcTop = arcTop;
        arcRadius = stageWidth * 1.68f;
        arcCenterY = arcTop + arcRadius;
        shaderHeight = 0;

        pillThickness = Math.max(dp(56), Math.min(dp(72), stageWidth * 0.17f));
        pillGap = dp(22);
        pillRadius = stageWidth * 1.72f;
        pillCenterY = arcTop - dp(16) - pillThickness / 2 + pillRadius;
        labelOffsetX = Math.min(stageWidth * 0.29f, dp(170));
        maxLabelWidth = Math.max(dp(48), 2 * Math.min(labelOffsetX - pillGap / 2 - dp(12), stageWidth / 2 - dp(8) - labelOffsetX));
        hasStage = true;
        invalidate();
    }

    /**
     * 弧形按钮上边缘最高点的 y 坐标
     */
    public float getPillTop() {
        return pillCenterY - pillRadius - pillThickness / 2;
    }

    /**
     * “取消”按钮文字中心的 x 坐标
     */
    public float getCancelCenterX() {
        return centerX - labelOffsetX;
    }

    /**
     * “转文字”按钮文字中心的 x 坐标
     */
    public float getTextCenterX() {
        return centerX + labelOffsetX;
    }

    /**
     * 手指所在的目标
     *
     * @return ZONE_SEND、ZONE_CANCEL 或 ZONE_TEXT
     */
    public int zoneAt(float x, float y) {
        if (!hasStage) {
            return ZONE_SEND;
        }
        float dx = x - centerX;
        if (Math.abs(dx) < arcRadius && y >= arcCenterY - (float) Math.sqrt(arcRadius * arcRadius - dx * dx)) {
            return ZONE_SEND;
        }
        return speechToTextEnabled && x >= centerX ? ZONE_TEXT : ZONE_CANCEL;
    }

    public int getZone() {
        return zone;
    }

    public void setZone(int zone) {
        if (this.zone == zone) {
            return;
        }
        this.zone = zone;
        float[] from = selection.clone();
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
        }
        selectionAnimator = ValueAnimator.ofFloat(0, 1);
        selectionAnimator.setDuration(220);
        selectionAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        selectionAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            for (int i = 0; i < selection.length; i++) {
                selection[i] = from[i] + ((i == zone ? 1 : 0) - from[i]) * fraction;
            }
            invalidate();
        });
        selectionAnimator.start();
    }

    /**
     * 恢复到未显示、选中发送的状态
     */
    public void reset() {
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
        }
        if (appearAnimator != null) {
            appearAnimator.cancel();
        }
        zone = ZONE_SEND;
        selection[ZONE_SEND] = 1;
        selection[ZONE_CANCEL] = 0;
        selection[ZONE_TEXT] = 0;
        appear = 0;
        invalidate();
    }

    /**
     * 弧形区域和按钮从底部升起
     */
    public void show() {
        animateAppear(1, 340, new DecelerateInterpolator(2f), null);
    }

    /**
     * 按钮和弧形区域落回底部
     */
    public void hide(@Nullable Runnable endAction) {
        animateAppear(0, 220, new AccelerateInterpolator(1.5f), endAction);
    }

    private void animateAppear(float target, long duration, @NonNull TimeInterpolator interpolator, @Nullable Runnable endAction) {
        if (appearAnimator != null) {
            appearAnimator.cancel();
        }
        appearAnimator = ValueAnimator.ofFloat(appear, target);
        appearAnimator.setDuration(duration);
        appearAnimator.setInterpolator(interpolator);
        appearAnimator.addUpdateListener(animation -> {
            appear = (float) animation.getAnimatedValue();
            invalidate();
        });
        appearAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!canceled && endAction != null) {
                    endAction.run();
                }
            }
        });
        appearAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
        }
        if (appearAnimator != null) {
            appearAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (!hasStage || appear <= 0) {
            return;
        }
        canvas.save();
        canvas.clipRect(stageLeft, 0, stageRight, getHeight());
        drawArcArea(canvas);
        // 按钮比弧形区域稍晚升起，更早落下
        float pillAppear = Math.max(0, Math.min(1, (appear - 0.15f) / 0.85f));
        if (pillAppear > 0) {
            canvas.save();
            canvas.translate(0, (1 - pillAppear) * (getHeight() - getPillTop()));
            drawPill(canvas, true, selection[ZONE_CANCEL], cancelLabel, cancelHint, pillAppear);
            if (speechToTextEnabled) {
                drawPill(canvas, false, selection[ZONE_TEXT], textLabel, textHint, pillAppear);
            }
            canvas.restore();
        }
        canvas.restore();
    }

    private void drawArcArea(@NonNull Canvas canvas) {
        if (shaderHeight != getHeight()) {
            shaderHeight = getHeight();
            arcSelectedPaint.setShader(new LinearGradient(0, arcTop, 0, Math.max(arcTop + 1, shaderHeight),
                ARC_SELECTED_TOP_COLOR, ARC_SELECTED_BOTTOM_COLOR, Shader.TileMode.CLAMP));
        }
        float selected = selection[ZONE_SEND];
        canvas.save();
        canvas.translate(0, (1 - appear) * (getHeight() - arcTop));
        canvas.drawCircle(centerX, arcCenterY, arcRadius, arcPaint);
        if (selected > 0) {
            arcSelectedPaint.setAlpha(Math.round(255 * selected));
            canvas.drawCircle(centerX, arcCenterY, arcRadius, arcSelectedPaint);
            rimPaint.setAlpha(Math.round(255 * selected));
            canvas.drawCircle(centerX, arcCenterY, arcRadius - density, rimPaint);
        }
        // “语音”和“松开 发送”交叉淡入淡出，文字同时轻微上移
        float labelCenterY = arcTop + dp(44) - dp(8) * selected;
        drawCenteredText(canvas, voiceLabel, labelCenterY, withAlpha(ARC_LABEL_COLOR, 1 - selected));
        drawCenteredText(canvas, sendLabel, labelCenterY, withAlpha(SELECTED_LABEL_COLOR, selected));
        canvas.restore();
    }

    private void drawCenteredText(@NonNull Canvas canvas, @NonNull String text, float centerY, int color) {
        if (Color.alpha(color) == 0) {
            return;
        }
        arcLabelPaint.setColor(color);
        Paint.FontMetrics fm = arcLabelPaint.getFontMetrics();
        canvas.drawText(text, centerX, centerY - (fm.ascent + fm.descent) / 2, arcLabelPaint);
    }

    private void drawPill(@NonNull Canvas canvas, boolean left, float selected, @NonNull String label, @NonNull String hint, float alpha) {
        // 选中时稍微变粗
        float thickness = pillThickness * (1 + 0.06f * selected);
        float innerDegrees = (float) Math.toDegrees(Math.asin((pillGap / 2 + pillThickness / 2) / pillRadius));
        float outerDegrees = (float) Math.toDegrees(Math.asin(Math.min(1, (stageWidth / 2 + thickness) / pillRadius)));
        // 圆的最高点是 270 度，角度顺时针增加，弧线从左往右画
        float startDegrees = left ? 270 - outerDegrees : 270 + innerDegrees;
        oval.set(centerX - pillRadius, pillCenterY - pillRadius, centerX + pillRadius, pillCenterY + pillRadius);
        pillPaint.setStrokeWidth(thickness);
        pillPaint.setColor(withAlpha(blend(PILL_COLOR, PILL_SELECTED_COLOR, selected), alpha));
        canvas.drawArc(oval, startDegrees, outerDegrees - innerDegrees, false, pillPaint);

        float offsetX = left ? -labelOffsetX : labelOffsetX;
        pillLabelPaint.setColor(withAlpha(blend(PILL_LABEL_COLOR, SELECTED_LABEL_COLOR, selected), alpha));
        drawTextOnArc(canvas, label, pillLabelPaint, pillLabelTextSize, pillRadius, offsetX, maxLabelWidth);
        if (selected > 0) {
            // 按钮上方的提示随高亮淡入，并向上浮起
            hintPaint.setColor(withAlpha(HINT_COLOR, alpha * selected));
            float hintRadius = pillRadius + thickness / 2 + dp(24) - dp(8) * (1 - selected);
            drawTextOnArc(canvas, hint, hintPaint, hintTextSize, hintRadius, offsetX, maxLabelWidth + dp(40));
        }
    }

    /**
     * 沿以 (centerX, pillCenterY) 为圆心的圆弧绘制文字，文字中心在 centerX + offsetX 附近，太长时缩小字号
     */
    private void drawTextOnArc(@NonNull Canvas canvas, @NonNull String text, @NonNull Paint paint, float textSize, float radius, float offsetX, float maxWidth) {
        paint.setTextSize(textSize);
        float width = paint.measureText(text);
        if (width > maxWidth) {
            paint.setTextSize(textSize * maxWidth / width);
            width = maxWidth;
        }
        // 路径两端多留一段，避免文字末尾超出路径被丢弃
        float margin = dp(24);
        float centerDegrees = 270 + (float) Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, offsetX / radius))));
        float halfDegrees = (float) Math.toDegrees((width / 2 + margin) / radius);
        oval.set(centerX - radius, pillCenterY - radius, centerX + radius, pillCenterY + radius);
        textPath.reset();
        textPath.addArc(oval, centerDegrees - halfDegrees, halfDegrees * 2);
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawTextOnPath(text, textPath, margin, -(fm.ascent + fm.descent) / 2, paint);
    }

    private int blend(int from, int to, float fraction) {
        return (int) argbEvaluator.evaluate(fraction, from, to);
    }

    private static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(Color.alpha(color) * Math.max(0, Math.min(1, alpha))), Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
