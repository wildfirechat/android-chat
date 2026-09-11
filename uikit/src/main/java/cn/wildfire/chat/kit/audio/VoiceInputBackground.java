/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 语音输入浮层的背景：整屏半透明的深色遮罩，底部再叠一块深灰色背景，上边缘从透明线性渐变到不透明。
 * 录音时深灰背景从弧形按钮处开始，编辑文字时升到气泡下方
 */
class VoiceInputBackground extends Drawable {
    private static final int DIM_COLOR = 0xCC111111;
    private static final int PANEL_COLOR = 0xFF444444;

    private final Paint dimPaint = new Paint();
    private final Paint panelPaint = new Paint();
    // 深灰背景上边缘渐变区域的高度
    private final float fadeHeight;
    private float stageLeft;
    private float stageRight;
    // 深灰背景完全不透明处的 y 坐标
    private float panelTop;
    private float progress;
    private LinearGradient panelShader;
    // panelShader 创建时的 panelTop
    private float shaderPanelTop;

    VoiceInputBackground(float fadeHeight) {
        this.fadeHeight = fadeHeight;
        dimPaint.setColor(DIM_COLOR);
    }

    /**
     * 深灰背景的左右边界，双栏时只覆盖会话界面
     */
    void setStage(float left, float right) {
        stageLeft = left;
        stageRight = right;
        invalidateSelf();
    }

    float getPanelTop() {
        return panelTop;
    }

    void setPanelTop(float panelTop) {
        this.panelTop = panelTop;
        invalidateSelf();
    }

    /**
     * @param progress 显示程度，0 完全透明，1 完全显示
     */
    void setProgress(float progress) {
        this.progress = progress;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (progress <= 0) {
            return;
        }
        Rect bounds = getBounds();
        dimPaint.setAlpha(Math.round(Color.alpha(DIM_COLOR) * progress));
        canvas.drawRect(bounds, dimPaint);
        if (stageRight <= stageLeft) {
            return;
        }
        float fadeTop = panelTop - fadeHeight;
        if (panelShader == null || shaderPanelTop != panelTop) {
            // 按实际坐标创建渐变，不用 localMatrix：系统强制深色时会重建渐变并丢掉 localMatrix，渐变变成一条硬边
            panelShader = new LinearGradient(0, fadeTop, 0, panelTop, PANEL_COLOR & 0x00FFFFFF, PANEL_COLOR, Shader.TileMode.CLAMP);
            panelPaint.setShader(panelShader);
            shaderPanelTop = panelTop;
        }
        panelPaint.setAlpha(Math.round(255 * progress));
        canvas.drawRect(stageLeft, Math.max(bounds.top, fadeTop), stageRight, bounds.bottom, panelPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        // 透明度由 setProgress 控制
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
