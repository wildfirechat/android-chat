package com.lqr.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CSDN_LQR
 * 图文混排工具
 */
public class MoonUtils {
    private static final float DEF_SCALE = 0.6f;
    private static final float SMALL_SCALE = 0.6F;

    /**
     * 具体类型的view设置内容
     *
     * @param textView
     * @param mSpannableString
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

    private static SpannableString replaceEmoticons(Context context, String value, float scale, int align) {
        if (TextUtils.isEmpty(value)) {
            value = "";
        }

        final char[] chars = value.toCharArray();
        final SpannableStringBuilder ssb = new SpannableStringBuilder(value);

        int codePoint;
        boolean isSurrogatePair;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                    isSurrogatePair = true;
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
                isSurrogatePair = false;
            }

            if (EmojiManager.contains(codePoint)) {
                Drawable d = getEmotDrawable(context, codePoint, scale);
                if (d != null) {
                    ImageSpan span = new ImageSpan(d, align);
                    ssb.setSpan(span, isSurrogatePair ? i - 1 : i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        return SpannableString.valueOf(ssb);
    }

    private static Pattern mATagPattern = Pattern.compile("<a.*?>.*?</a>");

    public static SpannableString makeSpannableStringTags(Context context, String value, float scale, int align) {
        return makeSpannableStringTags(context, value, DEF_SCALE, align, true);
    }

    public static SpannableString makeSpannableStringTags(Context context, String value, float scale, int align, boolean bTagClickable) {
        ArrayList<ATagSpan> tagSpans = new ArrayList<ATagSpan>();
        if (TextUtils.isEmpty(value)) {
            value = "";
        }
        //a标签需要替换原始文本,放在moonutil类中
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


        final char[] chars = value.toCharArray();
        final SpannableStringBuilder ssb = new SpannableStringBuilder(value);

        int codePoint;
        boolean isSurrogatePair;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                    isSurrogatePair = true;
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
                isSurrogatePair = false;
            }

            if (EmojiManager.contains(codePoint)) {
                Drawable d = getEmotDrawable(context, codePoint, scale);
                if (d != null) {
                    ImageSpan span = new ImageSpan(d, align);
                    ssb.setSpan(span, isSurrogatePair ? i - 1 : i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        return SpannableString.valueOf(ssb);
    }

    /**
     * 识别表情
     */
    public static void identifyFaceExpression(Context context,
                                              View textView, String value, int align) {
        identifyFaceExpression(context, textView, value, align, DEF_SCALE);
    }

    /**
     * 识别表情和标签（如：只需显示a标签对应的文本）
     */
    public static void identifyFaceExpressionAndATags(Context context,
                                                      View textView, String value, int align) {
        SpannableString mSpannableString = makeSpannableStringTags(context, value, DEF_SCALE, align);
        viewSetText(textView, mSpannableString);
    }

    /**
     * 识别表情，可设置缩放大小
     */
    public static void identifyFaceExpression(Context context,
                                              View textView, String value, int align, float scale) {
        SpannableString mSpannableString = replaceEmoticons(context, value, scale, align);
        viewSetText(textView, mSpannableString);
    }

    /**
     * 识别表情和标签（如：只需显示a标签对应的文本），可设置缩放大小
     */
    public static void identifyFaceExpressionAndTags(Context context,
                                                     View textView, String value, int align, float scale) {
        SpannableString mSpannableString = makeSpannableStringTags(context, value, scale, align, false);
        viewSetText(textView, mSpannableString);
    }

    /**
     * EditText用来转换表情文字的方法，如果没有使用EmoticonPickerView的attachEditText方法，则需要开发人员手动调用方法来又识别EditText中的表情
     */
    public static void replaceEmoticons(Context context, Editable editable, int start, int count) {
        if (count <= 0 || editable.length() < start + count)
            return;

        CharSequence s = editable.subSequence(start, start + count);

        final char[] chars = s.toString().toCharArray();

        int codePoint;
        boolean isSurrogatePair;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                    isSurrogatePair = true;
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
                isSurrogatePair = false;
            }

            if (EmojiManager.contains(codePoint)) {
                Drawable d = getEmotDrawable(context, codePoint, SMALL_SCALE);
                if (d != null) {
                    ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
                    editable.setSpan(span, isSurrogatePair ? i - 1 : i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private static Drawable getEmotDrawable(Context context, int code, float scale) {
        Drawable drawable = EmojiManager.getDrawable(context, code);

        // scale
        if (drawable != null) {
            int width = (int) (drawable.getIntrinsicWidth() * scale);
            int height = (int) (drawable.getIntrinsicHeight() * scale);
            drawable.setBounds(0, 0, width, height);
        }

        return drawable;
    }

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
                Uri uri = Uri.parse(mUrl);
                String scheme = uri.getScheme();
                if (TextUtils.isEmpty(scheme)) {
                    mUrl = "http://" + mUrl;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
