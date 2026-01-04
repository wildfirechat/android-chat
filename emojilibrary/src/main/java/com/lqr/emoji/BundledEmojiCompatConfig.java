package com.lqr.emoji;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.MetadataRepo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自定义 EmojiCompat 配置，从 assets 加载 Twemoji.ttf 字体文件
 *
 * 使用方法：
 * 1. 将 Twemoji.ttf 字体文件放在 emojilibrary/src/main/assets/ 目录下
 * 2. 调用 EmojiCompat.init(new BundledEmojiCompatConfig(context, "Twemoji.ttf"))
 */
public class BundledEmojiCompatConfig extends EmojiCompat.Config {

    /**
     * 构造函数
     *
     * @param context 上下文
     * @param fontFileName 字体文件名，例如 "Twemoji.ttf"
     */
    public BundledEmojiCompatConfig(@NonNull Context context, @NonNull String fontFileName) {
        super(new BundledMetadataLoader(context, fontFileName));
        setReplaceAll(true);
    }

    /**
     * 自定义 MetadataLoader，从 assets 加载字体文件
     */
    private static class BundledMetadataLoader implements EmojiCompat.MetadataRepoLoader {
        private final Context context;
        private final String fontFileName;
        private final ExecutorService executorService;

        BundledMetadataLoader(Context context, String fontFileName) {
            this.context = context.getApplicationContext();
            this.fontFileName = fontFileName;
            this.executorService = Executors.newSingleThreadExecutor();
        }

        @Override
        public void load(@NonNull EmojiCompat.MetadataRepoLoaderCallback callback) {
            executorService.execute(() -> {
                try {
                    // 从 assets 复制字体文件到缓存目录
                    File fontFile = copyAssetToCacheDir();

                    // 加载字体
                    Typeface typeface = Typeface.createFromFile(fontFile);

                    // 创建 MetadataRepo
                    MetadataRepo metadataRepo = MetadataRepo.create(typeface);

                    callback.onLoaded(metadataRepo);
                } catch (IOException e) {
                    callback.onFailed(new RuntimeException("Failed to load font file: " + fontFileName, e));
                } catch (Exception e) {
                    callback.onFailed(new RuntimeException("Failed to create MetadataRepo", e));
                }
            });
        }

        /**
         * 将 assets 中的字体文件复制到缓存目录
         */
        private File copyAssetToCacheDir() throws IOException {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                // 创建缓存文件
                File cacheDir = context.getCacheDir();
                File fontFile = new File(cacheDir, fontFileName);

                // 如果文件已存在且大小大于0，直接返回
                if (fontFile.exists() && fontFile.length() > 0) {
                    return fontFile;
                }

                // 从 assets 复制到缓存
                inputStream = assetManager.open(fontFileName);
                outputStream = new FileOutputStream(fontFile);

                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }

                outputStream.flush();
                return fontFile;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
