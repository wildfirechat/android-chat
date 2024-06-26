/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import androidx.annotation.Nullable;

import org.webrtc.VideoFrame;
import org.webrtc.VideoProcessor;
import org.webrtc.VideoSink;

public class TestVideoProcessor implements VideoProcessor {
    private VideoSink videoSink;

    @Override
    public void setSink(@Nullable VideoSink videoSink) {
        this.videoSink = videoSink;
    }

    @Override
    public void onCapturerStarted(boolean b) {
        // TODO
        // 一些初始化操作
    }

    @Override
    public void onCapturerStopped() {
        // TODO
        // 一些 清理化操作
    }


    @Override
    public void onFrameCaptured(VideoFrame videoFrame) {
        // TODO
        // 此处实现滤镜功能，之后，一定记得调用this.videoSink.onFrame(videoFrame)
        this.videoSink.onFrame(videoFrame);
    }
}
