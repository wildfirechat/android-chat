package cn.wildfirechat.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;

import cn.wildfirechat.model.VideoParam;

/**
 * @ClassName WeChatImageUtils
 * @Description  高仿微信的图片宽高比,获取小视频的参数
 * @Author dhl
 * @Date 2021/1/7 13:49
 * @Version 1.0
 */
public class WeChatImageUtils {

    private static final String TAG = "WeChatImageUtils";
    /**
     * 适当的宽高
     * @param outWidth
     * @param outHeight
     * @return
     */
    public static int[] getImageSizeByOrgSizeToWeChat(int outWidth, int outHeight) {
        int imageWidth = 300;
        int imageHeight = 300;
        int maxWidth = 400;
        int maxHeight = 400;
        int minWidth = 300;
        int minHeight = 250;
        if(outWidth == 0 && outHeight == 0){
            return new int[]{imageWidth,imageHeight};
        }
        if (outWidth / maxWidth > outHeight / maxHeight) {//
            if (outWidth >= maxWidth) {//
                imageWidth = maxWidth;
                imageHeight = outHeight * maxWidth / outWidth;
            } else {
                imageWidth = outWidth ;
                imageHeight = outHeight;

            }
            if (outHeight < minHeight) {
                imageHeight = minHeight;
                int width = outWidth * minHeight / outHeight;
                if (width > maxWidth) {
                    imageWidth = maxWidth;
                } else {
                    imageWidth = width;
                }
            }
        } else {
            if (outHeight >= maxHeight) {
                imageHeight = maxHeight;
                if (outHeight / maxHeight > 10) {
                    imageWidth = outWidth * 5 * maxHeight / outHeight;
                } else {
                    imageWidth = outWidth * maxHeight / outHeight;
                }

            } else {
                imageHeight = outHeight ;
                imageWidth = outWidth;
            }
            if (outWidth < minWidth) {
                imageWidth = minWidth ;
                int height = outHeight * minWidth / outWidth;
                if (height > maxHeight) {
                    imageHeight = maxHeight;
                } else {
                    imageHeight = height;
                }
            }
        }

        return new int[]{imageWidth,imageHeight};
    }

    /**
     * 获取图片的尺寸，只获取尺寸，不加载bitmap 不会耗时
     *
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
    public static VideoParam getVideoParam(String path){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        VideoParam videoParam = null;
        long duration = Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) ;
        Bitmap bitmap = retriever.getFrameAtTime();
        if(bitmap != null){
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] imageSize = WeChatImageUtils.getImageSizeByOrgSizeToWeChat(width,height);
            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, imageSize[0]/2, imageSize[1]/2);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            Log.e(TAG,"thumbnailBytes="+ baos.toByteArray().length/1024+"kb");
            videoParam = new VideoParam(width,height,duration,baos.toByteArray());
        }
        retriever.release();
        return videoParam;
    }

}
