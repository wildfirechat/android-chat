/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.utils;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;

public class WfcUtils {
    public static boolean isExternalTarget(String target) {
        if (target.contains("@")) {
            return target.split("@").length == 2;
        }

        return false;
    }

    public static String getExternalDomainId(String target) {
        if (target.contains("@")) {
            String[] cs = target.split("@");
            if (cs.length == 2) {
                return cs[1];
            }
        }
        return null;
    }

    public static SpannableString buildExternalDisplayNameSpannableString(String name, int spanSize) {
        SpannableString ss = new SpannableString(name);
        int start = name.indexOf("@");
        int end = name.length();
        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#F0A040")), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        ss.setSpan(new AbsoluteSizeSpan(spanSize, true), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return ss;
    }

}
