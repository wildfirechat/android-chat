/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

/**
 * 字体大小（字体缩放）工具类。
 * <p>
 * 参考微信「字体大小」功能，通过修改 {@link Configuration#fontScale} 实现全局字体缩放，
 * 与鸿蒙的 fontScale 思路一致。配置在各 Activity 的 attachBaseContext 以及 Application
 * 的 attachBaseContext 中统一应用，修改后需要重启 App 才能全局生效。
 */
public class FontScaleUtils {
    private static final String SP_FILE = "app_settings";
    private static final String FONT_SCALE_INDEX_PREF = "font_scale_index";

    /**
     * 可选的字体缩放级别，索引 {@link #DEFAULT_INDEX} 为标准。
     */
    public static final float[] FONT_SCALES = {0.85f, 1.0f, 1.15f, 1.30f, 1.45f};

    /**
     * 默认级别（标准）。
     */
    public static final int DEFAULT_INDEX = 1;

    public static int getFontScaleIndex(Context context) {
        int index = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE)
            .getInt(FONT_SCALE_INDEX_PREF, DEFAULT_INDEX);
        if (index < 0 || index >= FONT_SCALES.length) {
            index = DEFAULT_INDEX;
        }
        return index;
    }

    public static void setFontScaleIndex(Context context, int index) {
        if (index < 0 || index >= FONT_SCALES.length) {
            index = DEFAULT_INDEX;
        }
        context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(FONT_SCALE_INDEX_PREF, index)
            .apply();
    }

    public static float getFontScale(Context context) {
        return FONT_SCALES[getFontScaleIndex(context)];
    }

    /**
     * 在 {@code attachBaseContext} 中统一应用「语言 + 字体大小」设置的便捷方法。
     * <p>
     * Android 的资源配置是按 Context（每个 Activity）独立的，Application 级别的设置不会自动
     * 传递给各 Activity，因此每个 Activity 都需要在 {@code attachBaseContext} 调用本方法
     * （继承 {@link cn.wildfire.chat.kit.WfcBaseActivity} /
     * {@link cn.wildfire.chat.kit.WfcBaseNoToolbarActivity} 的页面已自动处理）。
     */
    public static Context wrap(Context base) {
        String language = LocaleUtils.getLanguage(base);
        Context context = LocaleUtils.updateResources(base, language);
        return applyFontScale(context);
    }

    /**
     * 将用户选择的字体缩放应用到给定 Context 的资源配置上，并返回应用后的 Context。
     * <p>
     * 基于当前配置进行复制并只修改 fontScale，从而保留已经设置好的语言（locale）等其它配置，
     * 需要在已应用语言设置（{@link LocaleUtils#updateResources}）之后链式调用。
     */
    public static Context applyFontScale(Context context) {
        float fontScale = getFontScale(context);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        if (configuration.fontScale == fontScale) {
            return context;
        }
        configuration.fontScale = fontScale;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(configuration);
        } else {
            Resources resources = context.getResources();
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
}
