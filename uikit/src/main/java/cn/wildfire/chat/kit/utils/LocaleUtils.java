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
    public static final String LANGUAGE_FOLLOW_SYSTEM = "follow_system";
    private static final String LANGUAGE_PREF = "language_preference";

    public static void setLocale(Context context, String language) {
        // 保存用户选择的语言
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_PREF, language)
            .apply();
    }

    public static String getLanguage(Context context) {
        String savedLanguage = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString(LANGUAGE_PREF, null);

        // 如果没有保存过语言设置，默认使用跟随系统
        if (savedLanguage == null) {
            savedLanguage = LANGUAGE_FOLLOW_SYSTEM;
        }

        // 如果是跟随系统，返回系统语言
        if (LANGUAGE_FOLLOW_SYSTEM.equals(savedLanguage)) {
            return getSystemLanguage();
        }

        return savedLanguage;
    }

    // 获取系统语言（返回标准化的语言代码）
    private static String getSystemLanguage() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = LocaleList.getDefault().get(0);
        } else {
            locale = Locale.getDefault();
        }

        String language = locale.getLanguage();
        // 标准化语言代码：中文系语言返回 "zh"，其他返回原语言代码
        if (language != null && language.startsWith("zh")) {
            return LANGUAGE_CHINESE;
        }
        // 如果是英语，返回 "en"，否则默认中文
        if (LANGUAGE_ENGLISH.equals(language)) {
            return LANGUAGE_ENGLISH;
        }
        // 其他语言默认返回中文
        return LANGUAGE_CHINESE;
    }

    // 获取实际保存的语言设置（可能是"follow_system"）
    public static String getSavedLanguage(Context context) {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString(LANGUAGE_PREF, LANGUAGE_FOLLOW_SYSTEM);
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
        } else if (LANGUAGE_CHINESE.equals(language)) {
            return Locale.SIMPLIFIED_CHINESE;
        } else {
            // 默认使用简体中文
            return Locale.SIMPLIFIED_CHINESE;
        }
    }
}
