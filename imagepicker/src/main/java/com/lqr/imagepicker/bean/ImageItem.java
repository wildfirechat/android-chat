package com.lqr.imagepicker.bean;

import java.io.Serializable;

public class ImageItem implements Serializable {

    public String name;
    public String path;
    public long size;
    public int width;
    public int height;
    public String mimeType;
    public long createTime;
    public boolean original;

    /**
     * 图片的路径和创建时间相同就认为是同一张图片
     */
    @Override
    public boolean equals(Object o) {
        try {
            ImageItem other = (ImageItem) o;
            return this.path.equalsIgnoreCase(other.path) && this.createTime == other.createTime;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return super.equals(o);
    }
}
