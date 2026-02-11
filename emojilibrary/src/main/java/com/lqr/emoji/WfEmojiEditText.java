/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package com.lqr.emoji;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji2.widget.EmojiEditText;

public class WfEmojiEditText extends EmojiEditText {

    private static final String TAG = "WfEmojiEditText";
    private boolean isProcessing = false;

    public WfEmojiEditText(@NonNull Context context) {
        super(context);
        init();
    }

    public WfEmojiEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WfEmojiEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 监听文本变化，动态添加 emoji 缩放效果
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isProcessing) {
                    scaleEmojis(s);
                }
            }
        });
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (text == null || text.length() == 0) {
            super.setText(text, type);
            return;
        }

        Log.d(TAG, "setText: " + text);

        try {
            isProcessing = true;
            
            // 创建 Spannable 用于添加缩放效果
            Spannable spannable;
            if (text instanceof Spannable) {
                spannable = (Spannable) text;
            } else {
                spannable = new SpannableStringBuilder(text);
            }

            scaleEmojis(spannable);
            
            super.setText(spannable, BufferType.SPANNABLE);
        } catch (Exception e) {
            Log.e(TAG, "Error processing emoji", e);
            super.setText(text, type);
        } finally {
            isProcessing = false;
        }
    }

    /**
     * 扫描文本并给所有 emoji 添加缩放效果
     */
    private void scaleEmojis(Spannable spannable) {
        if (spannable == null || spannable.length() == 0) {
            return;
        }

        try {
            isProcessing = true;
            
            // 直接扫描文本，检测 emoji 字符并添加缩放效果
            int emojiCount = 0;
            for (int i = 0; i < spannable.length(); ) {
                int codePoint = Character.codePointAt(spannable, i);
                int charCount = Character.charCount(codePoint);

                if (EmojiUtils.isEmoji(codePoint)) {
                    // 检查是否已经有缩放 Span
                    RelativeSizeSpan[] existing = spannable.getSpans(i, i + charCount, RelativeSizeSpan.class);
                    if (existing.length == 0) {
                        spannable.setSpan(
                            new RelativeSizeSpan(EmojiUtils.EMOJI_SCALE_FACTOR),
                            i,
                            i + charCount,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        emojiCount++;
                    }
                }

                i += charCount;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error scaling emojis", e);
        } finally {
            isProcessing = false;
        }
    }

}
