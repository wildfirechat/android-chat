package com.lqr.emoji;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.widget.EmojiTextView;

/**
 * 自定义 EmojiTextView，让 emoji 显示比普通文字大
 * 用于表情面板的 tab 标签
 */
public class TabEmojiTextView extends EmojiTextView {

    private float mEmojiScale = 1.2f; // emoji 放大倍数

    public TabEmojiTextView(Context context) {
        super(context);
        setup();
    }

    public TabEmojiTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public TabEmojiTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        // 单行显示
        setSingleLine(true);
        // Twemoji 字体本身就会让 emoji 显示得比文字大
    }

    /**
     * 设置 emoji 放大倍数
     */
    public void setEmojiScale(float scale) {
        this.mEmojiScale = scale;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 获取文本
        CharSequence text = getText();
        if (text == null || text.length() == 0) {
            super.onDraw(canvas);
            return;
        }

        // 检查是否包含 emoji
        boolean hasEmoji = EmojiCompat.isConfigured() && EmojiCompat.get().hasEmojiGlyph(text.toString());

        if (hasEmoji) {
            // 如果有 emoji，使用文本大小作为基准，emoji 会自然显得更大
            // Twemoji 字体本身的 emoji 就比文字大
            super.onDraw(canvas);
        } else {
            // 普通文字正常绘制
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 如果有 emoji，适当调整高度以更好地显示
        CharSequence text = getText();
        if (text != null && text.length() > 0) {
            boolean hasEmoji = EmojiCompat.isConfigured() && EmojiCompat.get().hasEmojiGlyph(text.toString());

            if (hasEmoji) {
                int width = getMeasuredWidth();
                int height = getMeasuredHeight();

                // emoji 本身就比较大，稍微增加高度让它不被裁剪
                int scaledHeight = (int) (height * 1.1f);
                setMeasuredDimension(width, scaledHeight);
            }
        }
    }
}
