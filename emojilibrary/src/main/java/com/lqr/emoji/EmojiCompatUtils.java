package com.lqr.emoji;

import android.content.Context;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.emoji2.text.EmojiCompat;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 使用 EmojiCompat 处理 Emoji 显示的工具类
 * 替代原有的 MoonUtils，使用 Twemoji 字体实现跨端表情统一
 */
public class EmojiCompatUtils {

    private static final float DEF_SCALE = 1.6f;
    private static Pattern mATagPattern = Pattern.compile("<a.*?>.*?</a>");

    /**
     * 识别表情并替换为 Twemoji
     *
     * @param context 上下文
     * @param textView 文本视图
     * @param value 文本内容
     * @param align 对齐方式（已废弃，保留用于兼容）
     */
    public static void identifyFaceExpression(Context context, View textView, String value, int align) {
        identifyFaceExpression(context, textView, value, align, DEF_SCALE);
    }

    /**
     * 识别表情并替换为 Twemoji，支持缩放
     *
     * @param context 上下文
     * @param textView 文本视图
     * @param value 文本内容
     * @param align 对齐方式（已废弃，保留用于兼容）
     * @param scale 缩放比例（已废弃，保留用于兼容）
     */
    public static void identifyFaceExpression(Context context, View textView, String value, int align, float scale) {
        if (textView instanceof TextView) {
            TextView tv = (TextView) textView;

            // 使用 EmojiCompat 处理文本
            CharSequence processedText = processText(context, value);

            tv.setText(processedText);
        } else if (textView instanceof EditText) {
            EditText et = (EditText) textView;
            CharSequence processedText = processText(context, value);
            et.setText(processedText);
        }
    }

    /**
     * 识别表情和标签（如：<a>标签）
     */
    public static void identifyFaceExpressionAndATags(Context context, View textView, String value, int align) {
        identifyFaceExpressionAndATags(context, textView, value, align, DEF_SCALE);
    }

    /**
     * 识别表情和标签，可设置缩放大小
     */
    public static void identifyFaceExpressionAndATags(Context context, View textView, String value, int align, float scale) {
        SpannableString spannableString = makeSpannableStringTags(context, value, scale, align, true);
        viewSetText(textView, spannableString);
    }

    /**
     * 识别表情和标签（不处理a标签点击）
     */
    public static void identifyFaceExpressionAndTags(Context context, View textView, String value, int align, float scale) {
        SpannableString spannableString = makeSpannableStringTags(context, value, scale, align, false);
        viewSetText(textView, spannableString);
    }

    /**
     * 处理文本，使用 EmojiCompat 替换 emoji
     */
    private static CharSequence processText(Context context, String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }

        // 使用 EmojiCompat 处理 emoji
        EmojiCompat emojiCompat = EmojiCompatManager.getEmojiCompat();
        if (emojiCompat != null && EmojiCompatManager.isInitialized()) {
            return emojiCompat.process(value);
        }

        return value;
    }

    /**
     * 处理带标签的文本
     */
    private static SpannableString makeSpannableStringTags(Context context, String value, float scale, int align, boolean bTagClickable) {
        ArrayList<ATagSpan> tagSpans = new ArrayList<>();
        if (TextUtils.isEmpty(value)) {
            value = "";
        }

        // 处理 <a> 标签
        Matcher aTagMatcher = mATagPattern.matcher(value);
        int start = 0;
        int end = 0;
        while (aTagMatcher.find()) {
            start = aTagMatcher.start();
            end = aTagMatcher.end();
            String atagString = value.substring(start, end);
            ATagSpan tagSpan = getTagSpan(atagString);
            value = value.substring(0, start) + tagSpan.getTag() + value.substring(end);
            tagSpan.setRange(start, start + tagSpan.getTag().length());
            tagSpans.add(tagSpan);
            aTagMatcher = mATagPattern.matcher(value);
        }

        // 使用 EmojiCompat 处理 emoji
        CharSequence processedText = processText(context, value);
        SpannableString spannableString;
        if (processedText instanceof SpannableString) {
            spannableString = (SpannableString) processedText;
        } else {
            spannableString = new SpannableString(processedText);
        }

        // 添加 <a> 标签的 span
        for (ATagSpan tagSpan : tagSpans) {
            if (bTagClickable) {
                spannableString.setSpan(tagSpan, tagSpan.start, tagSpan.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return spannableString;
    }

    /**
     * 具体类型的 view 设置内容
     */
    private static void viewSetText(View textView, SpannableString mSpannableString) {
        if (textView instanceof TextView) {
            TextView tv = (TextView) textView;
            tv.setText(mSpannableString);
        } else if (textView instanceof EditText) {
            EditText et = (EditText) textView;
            et.setText(mSpannableString);
        }
    }

    /**
     * EditText 用来转换表情文字的方法
     */
    public static void replaceEmoticons(Context context, Editable editable, int start, int count) {
        // EmojiCompat 会自动处理，无需手动替换
        // 保留此方法用于兼容性
    }

    /**
     * 解析 <a> 标签
     */
    private static ATagSpan getTagSpan(String text) {
        String href = null;
        String tag = null;
        if (text.toLowerCase().contains("href")) {
            int start = text.indexOf("\"");
            int end = text.indexOf("\"", start + 1);
            if (end > start)
                href = text.substring(start + 1, end);
        }
        int start = text.indexOf(">");
        int end = text.indexOf("<", start);
        if (end > start)
            tag = text.substring(start + 1, end);
        return new ATagSpan(tag, href);
    }

    /**
     * <a> 标签的 ClickableSpan
     */
    private static class ATagSpan extends ClickableSpan {
        private int start;
        private int end;
        private String mUrl;
        private String tag;

        ATagSpan(String tag, String url) {
            this.tag = tag;
            this.mUrl = url;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(true);
        }

        public String getTag() {
            return tag;
        }

        public void setRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public void onClick(View widget) {
            try {
                if (TextUtils.isEmpty(mUrl))
                    return;
                // 可以在这里处理链接点击事件
                android.util.Log.d("EmojiCompatUtils", "Link clicked: " + mUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
