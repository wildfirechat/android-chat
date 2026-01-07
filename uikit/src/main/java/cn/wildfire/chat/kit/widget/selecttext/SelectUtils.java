/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget.selecttext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类
 * hxg 2023.1.5 qq:929842234@qq.com
 */
public class SelectUtils {
    private static int STATUS_HEIGHT = 0;

    /**
     * 替换内容
     *
     * @param stringBuilder      SpannableStringBuilder text
     * @param mOriginalContent   CharSequence text
     * @param targetText         Target Text
     * @param replaceText        Replace Text
     */
    public static void replaceContent(
            SpannableStringBuilder stringBuilder,
            CharSequence mOriginalContent,
            String targetText,
            String replaceText
    ) {
        int startIndex = mOriginalContent.toString().indexOf(targetText);
        if (startIndex != -1) {
            int endIndex = startIndex + targetText.length();
            stringBuilder.replace(startIndex, endIndex, replaceText);
        }
    }

    /**
     * 文字转化成图片背景
     *
     * @param context       Context
     * @param emojiMap      Emoji map
     * @param stringBuilder SpannableStringBuilder text
     * @param content       Target content
     */
    public static void replaceText2Emoji(
            Context context,
            java.util.Map<String, Integer> emojiMap,
            SpannableStringBuilder stringBuilder,
            CharSequence content
    ) {
        if (emojiMap.isEmpty()) {
            return;
        }
        for (String key : emojiMap.keySet()) {
            int drawableRes = emojiMap.get(key);
            Pattern pattern = Pattern.compile(key);
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
                if (drawable == null) continue;

                // 动画图（加载多张 Drawable 图片资源组合而成的动画）
                if (drawable instanceof AnimationDrawable) {
                    ((AnimationDrawable) drawable).start(); // 开始播放动画
                }

                // 动态图
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (drawable instanceof AnimatedImageDrawable) {
                        ((AnimatedImageDrawable) drawable).start();
                    }
                }

                // 动态矢量图
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (drawable instanceof AnimatedVectorDrawable) {
                        ((AnimatedVectorDrawable) drawable).start();
                    }
                }

                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                SelectImageSpan span = new SelectImageSpan(
                        drawable, Color.TRANSPARENT, DynamicDrawableSpan.ALIGN_CENTER
                );
                stringBuilder.setSpan(span, start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static int getPreciseOffset(TextView textView, int x, int y) {
        Layout layout = textView.getLayout();
        if (layout != null) {
            int topVisibleLine = layout.getLineForVertical(y);
            int offset = layout.getOffsetForHorizontal(topVisibleLine, x);
            int offsetX = (int) layout.getPrimaryHorizontal(offset);
            if (offsetX > x) {
                return layout.getOffsetToLeftOf(offset);
            } else {
                return offset;
            }
        }
        return -1;
    }

    public static int getHysteresisOffset(TextView textView, int x, int y, int previousOffset) {
        Layout layout = textView.getLayout();
        if (layout == null) return -1;

        int line = layout.getLineForVertical(y);

        if (isEndOfLineOffset(layout, previousOffset)) {
            int left = (int) layout.getPrimaryHorizontal(previousOffset - 1);
            int right = (int) layout.getLineRight(line);
            int threshold = (right - left) / 2;
            if (x > right - threshold) {
                previousOffset -= 1;
            }
        }

        int previousLine = layout.getLineForOffset(previousOffset);
        int previousLineTop = layout.getLineTop(previousLine);
        int previousLineBottom = layout.getLineBottom(previousLine);
        int hysteresisThreshold = (previousLineBottom - previousLineTop) / 2;

        if ((line == previousLine + 1 && y - previousLineBottom < hysteresisThreshold) ||
                (line == previousLine - 1 && (previousLineTop - y) < hysteresisThreshold)) {
            line = previousLine;
        }

        int offset = layout.getOffsetForHorizontal(line, x);

        if (offset < textView.getText().length() - 1) {
            int right = (int) layout.getLineRight(line);
            boolean isEnd = x >= right;
            if (isEnd) {
                int left = (int) layout.getPrimaryHorizontal(offset);
                int threshold = (right - left) / 2;
                if (x > right - threshold) {
                    int index = getLastTextLength(layout, offset);
                    offset += index;
                }
            }
        }
        return offset;
    }

    /**
     * 得到最后一个字符长度
     */
    private static int getLastTextLength(Layout layout, int offset) {
        int index = 1;
        for (int i = 1; i <= 20; i++) {
            if (isEndOfLineOffset(layout, offset + i)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private static boolean isEndOfLineOffset(Layout layout, int offset) {
        return offset > 0 && layout.getLineForOffset(offset) == layout.getLineForOffset(offset - 1) + 1;
    }

    public static int getDisplayWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getDisplayHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static int dp2px(float dpValue) {
        return (int) (dpValue * Resources.getSystem().getDisplayMetrics().density + 0.5f);
    }

    /**
     * 设置宽高
     *
     * @param v View
     * @param w width
     * @param h height
     */
    public static void setWidthHeight(View v, int w, int h) {
        ViewGroup.LayoutParams params = v.getLayoutParams();
        params.width = w;
        params.height = h;
        v.setLayoutParams(params);
    }

    /**
     * 获取通知栏的高度
     */
    public static int getStatusHeight() {
        if (STATUS_HEIGHT != 0) {
            return STATUS_HEIGHT;
        }
        int resid = Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android");
        if (resid > 0) {
            STATUS_HEIGHT = Resources.getSystem().getDimensionPixelSize(resid);
            return STATUS_HEIGHT;
        }
        return -1;
    }

    /**
     * 反射获取对象属性值
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || TextUtils.isEmpty(fieldName)) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        while (!clazz.equals(Object.class)) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception ignore) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * 判断是否为emoji表情符
     *
     * @param c 字符
     * @return 是否为emoji字符
     */
    public static boolean isEmojiText(char c) {
        int code = c;
        return !(code == 0x0 || code == 0x9 || code == 0xA || code == 0xD ||
                (code >= 0x20 && code <= 0xD7FF) ||
                (code >= 0xE000 && code <= 0xFFFD) ||
                (code >= 0x100000 && code <= 0x10FFFF));
    }

    /**
     * 利用反射检测文本是否是ImageSpan文本
     */
    public static boolean isImageSpanText(Spannable mSpannable) {
        if (TextUtils.isEmpty(mSpannable)) {
            return false;
        }
        try {
            Object mSpans = getFieldValue(mSpannable, "mSpans");
            if (mSpans != null && mSpans instanceof Object[]) {
                for (Object mSpan : (Object[]) mSpans) {
                    if (mSpan instanceof SelectImageSpan) {
                        return true;
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    /**
     * 匹配Image
     *
     * @param emojiMap Emoji picture map
     * @param content  Target content
     */
    public static boolean matchImageSpan(java.util.Map<String, Integer> emojiMap, String content) {
        if (emojiMap.isEmpty()) {
            return false;
        }
        for (String key : emojiMap.keySet()) {
            Pattern pattern = Pattern.compile(key);
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }
}
