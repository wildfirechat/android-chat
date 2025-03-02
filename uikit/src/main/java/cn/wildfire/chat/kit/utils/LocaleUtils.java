/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;

import java.util.Locale;

public class LocaleUtils {
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_CHINESE = "zh";
    private static final String LANGUAGE_PREF = "language_preference";

    public static void setLocale(Context context, String language) {
        // 保存用户选择的语言
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_PREF, language)
            .apply();
    }

    public static String getLanguage(Context context) {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString(LANGUAGE_PREF, LANGUAGE_CHINESE); // 默认使用中文
    }

    // 修改 updateResources 方法，确保所有资源配置都被正确更新
    public static Context updateResources(Context context, String language) {
        if (TextUtils.isEmpty(language)) {
            return context;
        }
        Locale locale = getLocaleFromLanguage(language);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLocales(new LocaleList(locale));
            return context.createConfigurationContext(configuration);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }

        // 确保在所有 Android 版本中都更新资源配置
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    private static Locale getLocaleFromLanguage(String language) {
        if (LANGUAGE_ENGLISH.equals(language)) {
            return Locale.ENGLISH;
        } else {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }
}
