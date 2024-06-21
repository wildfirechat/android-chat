/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.utils;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
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

    public static SpannableString buildExternalDisplayNameSpannableString(String name) {
        SpannableString ss = new SpannableString(name);
        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#F0A040")), name.indexOf("@"), name.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return ss;
    }
}
