/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.imagerecommend;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * 图片推荐管理器
 * 查询最近的截图并推荐
 */
public class ImageRecommendManager {
    private static ImageRecommendManager sInstance;

    private Set<String> mRecommendedImages;  // 去重

    private static final long TIME_WINDOW_MS = 60000;  // 查询1分钟内的图片
    private static final String SCREENSHOT_PATH_KEYWORD = "screenshot";

    private ImageRecommendManager() {
        mRecommendedImages = new HashSet<>();
    }

    public static synchronized ImageRecommendManager getInstance() {
        if (sInstance == null) {
            sInstance = new ImageRecommendManager();
        }
        return sInstance;
    }

    /**
     * 清理已推荐图片列表
     * 在用户离开会话时调用，避免跨会话的图片被过滤
     */
    public void clearRecommendedImages() {
        Log.d("ImageRecommend", "clearRecommendedImages: clearing " + mRecommendedImages.size() + " items");
        mRecommendedImages.clear();
    }

    /**
     * 查询最新图片
     */
    public void queryRecentImages(Context context, OnImageRecommendCallback callback) {
        if (callback == null) {
            return;
        }
        String[] projection = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        };

        // 查询过去1分钟内的图片
        long now = System.currentTimeMillis();
        String selection = MediaStore.Images.Media.DATE_ADDED + " > ?";
        String timeWindowSeconds = String.valueOf((now - TIME_WINDOW_MS) / 1000);

        Log.d("ImageRecommend", "queryLatestImage: now=" + now + ", timeWindowSeconds=" + timeWindowSeconds);

        try (Cursor cursor = context.getContentResolver().query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            new String[]{timeWindowSeconds},
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {

            if (cursor != null && cursor.moveToFirst()) {
                int count = 0;
                int maxCount = 10;  // 最多检查 10 张图片
                Log.d("ImageRecommend", "queryLatestImage: found " + cursor.getCount() + " images");
                do {
                    String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                    long dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)) * 1000;

                    Log.d("ImageRecommend", "checking image: path=" + imagePath + ", dateAdded=" + dateAdded);

                    // 检查是否是截图
                    boolean isScreenshot = isScreenshot(imagePath);
                    Log.d("ImageRecommend", "isScreenshot=" + isScreenshot);

//                    if (isScreenshot) {
                        // 去重检查
                        if (!mRecommendedImages.contains(imagePath)) {
                            Log.d("ImageRecommend", "FOUND MATCH! Notifying listener");
                            mRecommendedImages.add(imagePath);
                            callback.onNewImageRecommended(imagePath, null);
                            break;
                        } else {
                            Log.d("ImageRecommend", "image already recommended");
                        }
//                    }

                    count++;
                } while (cursor.moveToNext() && count < maxCount);
            } else {
                Log.d("ImageRecommend", "queryLatestImage: no images found or cursor is null");
            }
        } catch (Exception e) {
            Log.e("ImageRecommend", "Error querying images", e);
        }
    }

    /**
     * 判断是否是截图
     */
    private boolean isScreenshot(String path) {
        if (path == null) {
            return false;
        }
        String lowerPath = path.toLowerCase();
        return lowerPath.contains(SCREENSHOT_PATH_KEYWORD);
    }

    /**
     * 图片推荐监听器
     */
    public interface OnImageRecommendCallback {
        void onNewImageRecommended(String imagePath, Uri imageUri);
    }
}
