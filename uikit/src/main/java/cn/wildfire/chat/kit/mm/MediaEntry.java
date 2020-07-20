package cn.wildfire.chat.kit.mm;

import android.graphics.Bitmap;

public class MediaEntry {
    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_VIDEO = 1;
    private int type;
    private String mediaUrl;
    private String mediaLocalPath;
    private String thumbnailUrl;
    // TODO 消息里的缩略图会被移除
    private Bitmap thumbnail;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaLocalPath() {
        return mediaLocalPath;
    }

    public void setMediaLocalPath(String mediaLocalPath) {
        this.mediaLocalPath = mediaLocalPath;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }
}
