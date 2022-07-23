/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import androidx.annotation.Nullable;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.webrtc.VideoFrame;
import org.webrtc.VideoProcessor;
import org.webrtc.VideoSink;

import java.nio.ByteBuffer;

/**
 * 测试目录
 * 将视频流处理为黑白
 */
public class GrayScaleOpencvVideoProcessor implements VideoProcessor {
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

    // opencv implement example
    @Override
    public void onFrameCaptured(VideoFrame videoFrame) {
        // 转成 Mat
        VideoFrame.I420Buffer i420Buffer = videoFrame.getBuffer().toI420();
        ByteBuffer dataY = i420Buffer.getDataY();
        ByteBuffer dataU = i420Buffer.getDataU();
        ByteBuffer dataV = i420Buffer.getDataV();

        int width = videoFrame.getRotatedWidth();
        int height = videoFrame.getRotatedHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(dataY.limit() + dataU.limit() + dataV.limit());
        byteBuffer.put(dataY);
        byteBuffer.put(dataU);
        byteBuffer.put(dataV);

        Mat mat = new Mat((int) (height * 1.5), width, CvType.CV_8UC1);
        mat.put(0, 0, byteBuffer.array());

        Mat bgrMat = new Mat();
        Imgproc.cvtColor(mat, bgrMat, Imgproc.COLOR_YUV2BGR_I420);
        mat.release();

        //  图像处理处理
        Mat grayScaleMat = new Mat();
        Imgproc.cvtColor(bgrMat, grayScaleMat, Imgproc.COLOR_BGR2GRAY);

        bgrMat.release();
        bgrMat = new Mat();
        Imgproc.cvtColor(grayScaleMat, bgrMat, Imgproc.COLOR_GRAY2BGR);
        grayScaleMat.release();

        Imgproc.circle(bgrMat, new Point(bgrMat.width() / 2, bgrMat.height() / 2), 50, new Scalar(255.0, 0.0, 0.0));

        // 转成 yuv
        Mat yuvMat = new Mat();
        Imgproc.cvtColor(bgrMat, yuvMat, Imgproc.COLOR_BGR2YUV_I420);
        byte[] resultData = new byte[(int) (width * height * 1.5)];
        yuvMat.get(0, 0, resultData);
        yuvMat.release();
        bgrMat.release();

        dataY.clear();
        dataU.clear();
        dataV.clear();

        int position = 0;
        for (int i = position; i < width * height; i++) {
            dataY.put(resultData[i]);
        }

        position = position + width * height;
        for (int i = position; i < position + (width * height) / 4; i++) {
            dataU.put(resultData[i]);
        }

        position = position + (width * height) / 4;
        for (int i = position; i < resultData.length; i++) {
            dataV.put(resultData[i]);
        }

        this.videoSink.onFrame(new VideoFrame(i420Buffer, videoFrame.getRotation(), videoFrame.getTimestampNs()));
        videoFrame.release();
    }
}
