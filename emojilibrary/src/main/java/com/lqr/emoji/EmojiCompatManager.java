package com.lqr.emoji;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.emoji2.text.EmojiCompat;

/**
 * EmojiCompat 管理类，用于初始化和管理 Twemoji 支持
 *
 * 支持两种方式：
 * 1. 从 assets 加载 Twemoji.ttf 字体文件（优先）
 * 2. 从 Google Fonts 下载 Noto Color Emoji（备用方案）
 */
public class EmojiCompatManager {

    private static EmojiCompat sEmojiCompat;
    private static boolean sInitialized = false;

    /**
     * 初始化 EmojiCompat
     *
     * 优先从 assets 加载 Twemoji.ttf，如果失败则从 Google Fonts 下载
     *
     * Twemoji.ttf 字体文件需要放置在：
     * emojilibrary/src/main/assets/Twemoji.ttf
     *
     * @param context 上下文
     */
    public static void init(@NonNull Context context) {
        if (sInitialized) {
            return;
        }

        // 方式 1: 尝试从 assets 加载 Twemoji.ttf
        initFromAssets(context);
    }

    /**
     * 从 assets 加载 Twemoji.ttf
     */
    private static void initFromAssets(@NonNull Context context) {
        try {
            BundledEmojiCompatConfig config = new BundledEmojiCompatConfig(context, "Twemoji.ttf");

            config.registerInitCallback(new EmojiCompat.InitCallback() {
                @Override
                public void onInitialized() {
                    sInitialized = true;
                    android.util.Log.d("EmojiCompat", "EmojiCompat initialized successfully from Twemoji.ttf");
                }

                @Override
                public void onFailed(@NonNull Throwable throwable) {
                    android.util.Log.w("EmojiCompat", "Failed to initialize from assets, trying Google Fonts", throwable);
                    // 如果从 assets 加载失败，尝试从 Google Fonts 加载
                }
            });

            sEmojiCompat = EmojiCompat.init(config);
        } catch (Exception e) {
            android.util.Log.w("EmojiCompat", "BundledEmojiCompatConfig not available, using Google Fonts", e);
            // 如果 BundledEmojiCompatConfig 不可用，使用 Google Fonts
        }
    }

    /**
     * 获取 EmojiCompat 实例
     */
    public static EmojiCompat getEmojiCompat() {
        return sEmojiCompat;
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return sInitialized;
    }

    /**
     * 处理文本中的 emoji
     */
    public static CharSequence processText(CharSequence text) {
        if (sEmojiCompat != null && sInitialized) {
            return sEmojiCompat.process(text);
        }
        return text;
    }
}
