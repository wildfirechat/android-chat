package com.cjt2325.cameralibrary.listener;

/**
 * create by CJT2325
 * 445263848@qq.com.
 */

public interface CaptureListener {
    void takePictures();

    void recordShort(long time);

    void recordStart();

    void recordEnd(long time);

    void recordZoom(float zoom);

    void recordError();
}
