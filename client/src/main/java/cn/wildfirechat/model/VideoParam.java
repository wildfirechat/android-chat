package cn.wildfirechat.model;

import android.graphics.Bitmap;

/**
 * 视频参数类
 * <p>
 * 用于表示小视频的参数信息。
 * 包含视频宽度、高度、时长和缩略图字节数组。
 * </p>
 *
 * @author WildFireChat
 * @since 2021
 */
public class VideoParam {

    /**
     * 视频宽度
     */
    private int width ;

    /**
     * 视频高度
     */
    private int height;

    /**
     * 视频时长（毫秒）
     */
    private long duration ;

    /**
     * 缩略图字节数组
     */
    private byte[] thumbnailBytes ;
        public VideoParam(int width, int height, long duration, byte[] thumbnailBytes ){
            this.width = width ;
            this.height = height;
            this.duration = duration ;
            this.thumbnailBytes = thumbnailBytes ;
        }


    public long getDuration() {
        return duration;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public byte[] getThumbnailBytes() {
        return thumbnailBytes;
    }


}
