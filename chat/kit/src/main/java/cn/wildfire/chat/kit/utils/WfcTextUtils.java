package cn.wildfire.chat.kit.utils;

import android.text.Html;
import android.text.Spanned;

public class WfcTextUtils {
    public static String htmlToText(String html) {
        if (html == null || !html.startsWith("<")) {
            return html;
        }
        Spanned spanned = Html.fromHtml(html);
        char[] chars = new char[spanned.length()];
        android.text.TextUtils.getChars(spanned, 0, spanned.length(), chars, 0);
        String plainText = new String(chars);
        return plainText;
    }
}
