package com.lqr.emoji;

import android.content.Context;
import android.widget.ImageView;

/**
 * CSDN_LQR
 * 图片加载器（交由外部工程决定）
 */
public interface IImageLoader {

    void displayImage(Context context, String path, ImageView imageView);
}
