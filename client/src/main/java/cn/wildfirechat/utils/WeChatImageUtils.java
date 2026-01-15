package cn.wildfirechat.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import cn.wildfirechat.model.VideoParam;

/**
 * @ClassName WeChatImageUtils
 * @Description 高仿微信的图片宽高比, 获取小视频的参数
 * @Author dhl
 * @Date 2021/1/7 13:49
 * @Version 1.0
 */
public class WeChatImageUtils {

    private static final String TAG = "WeChatImageUtils";

    /**
     * 计算图片、视频消息在消息列表合适的展示宽高
     *
     * @param orgWidth 图片原始宽度
     * @param orgHeight 图片原始高度
     * @return 在消息列表里面合适的展示尺寸
     */
    public static int[] getImageSizeByOrgSizeToWeChat(int orgWidth, int orgHeight) {
        int imageWidth = 300;
        int imageHeight = 300;
        int maxWidth = 400;
        int maxHeight = 400;
        int minWidth = 300;
        int minHeight = 250;
        if (orgWidth == 0 && orgHeight == 0) {
            return new int[]{imageWidth, imageHeight};
        }
        if (orgWidth / maxWidth > orgHeight / maxHeight) {//
            if (orgWidth >= maxWidth) {//
                imageWidth = maxWidth;
                imageHeight = orgHeight * maxWidth / orgWidth;
            } else {
                imageWidth = orgWidth;
                imageHeight = orgHeight;

            }
            if (orgHeight < minHeight) {
                imageHeight = minHeight;
                int width = orgWidth * minHeight / orgHeight;
                if (width > maxWidth) {
                    imageWidth = maxWidth;
                } else {
                    imageWidth = width;
                }
            }
        } else {
            if (orgHeight >= maxHeight) {
                imageHeight = maxHeight;
                if (orgHeight / maxHeight > 10) {
                    imageWidth = orgWidth * 5 * maxHeight / orgHeight;
                } else {
                    imageWidth = orgWidth * maxHeight / orgHeight;
                }

            } else {
                imageHeight = orgHeight;
                imageWidth = orgWidth;
            }
            if (orgWidth < minWidth) {
                imageWidth = minWidth;
                int height = orgHeight * minWidth / orgWidth;
                if (height > maxHeight) {
                    imageHeight = maxHeight;
                } else {
                    imageHeight = height;
                }
            }
        }

        return new int[]{Math.min(imageWidth, maxWidth), Math.min(imageHeight, maxHeight)};
    }

    /**
     * 获取图片的尺寸，只获取尺寸，不加载bitmap 不会耗时
     */
    public static int[] getSize(File file) {
        if (file == null) return new int[]{0, 0};
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        return new int[]{opts.outWidth, opts.outHeight};
    }
    /**
     * 获取小视频的参数的尺寸，时长
     *
     */

    /**
     * 获取小视频的参数
     * 通过第一帧获取宽高
     * 通过源文件获取会有点小问题
     */
    public static VideoParam getVideoParam(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        VideoParam videoParam = null;
        long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        Bitmap bitmap = retriever.getFrameAtTime();
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] imageSize = WeChatImageUtils.getImageSizeByOrgSizeToWeChat(width, height);
            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, imageSize[0] / 2, imageSize[1] / 2);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            Log.e(TAG, "thumbnailBytes=" + baos.toByteArray().length / 1024 + "kb");
            videoParam = new VideoParam(width, height, duration, baos.toByteArray());
        }
        try {
            retriever.release();
        } catch (IOException e) {
            e.printStackTrace();
            videoParam = new VideoParam(0, 0, 0, new byte[0]);
        }
        return videoParam;
    }

}
