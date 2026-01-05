/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package com.lqr.emoji;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji2.widget.EmojiTextView;

public class WfEmojiTextView extends EmojiTextView {

    private static final String TAG = "WfEmojiTextView";

    public WfEmojiTextView(@NonNull Context context) {
        super(context);
    }

    public WfEmojiTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WfEmojiTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (text == null || text.length() == 0) {
            super.setText(text, type);
            return;
        }

        Log.d(TAG, "setText: " + text);

        try {
            // 创建 Spannable 用于添加缩放效果
            Spannable spannable;
            if (text instanceof Spannable) {
                spannable = (Spannable) text;
            } else {
                spannable = new SpannableStringBuilder(text);
            }

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

            Log.d(TAG, "Scaled " + emojiCount + " emojis");
            super.setText(spannable, BufferType.SPANNABLE);
        } catch (Exception e) {
            Log.e(TAG, "Error processing emoji", e);
            super.setText(text, type);
        }
    }

}
