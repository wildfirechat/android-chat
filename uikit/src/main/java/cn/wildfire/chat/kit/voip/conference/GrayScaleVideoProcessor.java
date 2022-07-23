/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import androidx.annotation.Nullable;

import org.webrtc.VideoFrame;
import org.webrtc.VideoProcessor;
import org.webrtc.VideoSink;

import java.nio.ByteBuffer;

/**
 * 测试目录
 * 将视频流处理为黑白
 */
public class GrayScaleVideoProcessor implements VideoProcessor {
    private VideoSink videoSink;

    @Override
    public void setSink(@Nullable VideoSink videoSink) {
        this.videoSink = videoSink;
    }

    @Override
    public void onCapturerStarted(boolean b) {

    }

    @Override
    public void onCapturerStopped() {

    }

    @Override
    public void onFrameCaptured(VideoFrame videoFrame) {
        VideoFrame.I420Buffer i420Buffer = videoFrame.getBuffer().toI420();
        ByteBuffer dataU = i420Buffer.getDataU();
        ByteBuffer dataV = i420Buffer.getDataV();
        for (int i = 0; i < dataU.limit(); i++) {
            dataU.put((byte) 127);
        }
        for (int i = 0; i < dataV.limit(); i++) {
            dataV.put((byte) 127);
        }

        this.videoSink.onFrame(new VideoFrame(i420Buffer, videoFrame.getRotation(), videoFrame.getTimestampNs()));
    }
}
