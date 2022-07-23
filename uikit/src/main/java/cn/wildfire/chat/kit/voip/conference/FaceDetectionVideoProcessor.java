/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;

import androidx.annotation.Nullable;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.webrtc.VideoFrame;
import org.webrtc.VideoProcessor;
import org.webrtc.VideoSink;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import cn.wildfire.chat.kit.R;

/**
 * 测试目录
 * 将视频流处理为黑白
 */
public class FaceDetectionVideoProcessor implements VideoProcessor {
    private VideoSink videoSink;
    private CascadeClassifier faceDetector;

    FaceDetectionVideoProcessor(Context context) {
        super();
        try {
            this.faceDetector = this.loadFaceLib(context);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private CascadeClassifier loadFaceLib(Context context) throws IOException {
        File dir = context.getDir("facelib", Context.MODE_PRIVATE);
        File faceModelFile = new File(dir, "lbpcascade_frontalface.xml");
        if (!faceModelFile.exists()) {
            InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            FileOutputStream fos = new FileOutputStream(faceModelFile);
            byte[] bytes = new byte[4096];
            int len = is.read(bytes);
            while (len > 0) {
                fos.write(bytes, 0, len);
                len = is.read(bytes);
            }
            fos.close();
            is.close();
        }

        return new CascadeClassifier(faceModelFile.getAbsolutePath());
    }

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
//        if (this.faceDetector != null) {
//            Mat grayScaleMat = new Mat();
//            Imgproc.cvtColor(bgrMat, grayScaleMat, Imgproc.COLOR_BGR2GRAY);
//            MatOfRect faceRects = new MatOfRect();
//            this.faceDetector.detectMultiScale(grayScaleMat, faceRects);
//            for (Rect rect : faceRects.toArray()) {
//
//                int x = rect.x;
//                int y = rect.y;
//                int w = rect.width;
//                int h = rect.height;
//
//                Log.e("jjjjjjjjj", "x: " + x + " " + y + " " + w + " " + h);
//
//                Imgproc.rectangle(
//                    bgrMat,
//                    new Point(x, y),
//                    new Point(w, h),
//                    new Scalar(255.0, 0.0, 0.0)
//                );
//            }
//            grayScaleMat.release();
//        }

//        Imgproc.rectangle(
//            bgrMat,
//            new Point(50, 50),
//            new Point(200, 200),
//            new Scalar(255.0, 0.0, 0.0),
//            5
//        );

        // 转成 yuv
        Mat yuvMat = new Mat();
        Imgproc.cvtColor(bgrMat, yuvMat, Imgproc.COLOR_BGR2YUV_I420);
        byte[] resultData = new byte[(int) (width * height * 1.5)];
        yuvMat.get(0, 0, resultData);
        yuvMat.release();
        bgrMat.release();

        int position = 0;
        dataY.clear();
//        for (int i = position; i < width * height; i++) {
//            dataY.put(resultData[i]);
//        }
        dataY.put(resultData, position, width * height);

        position = width * height;
        dataU.clear();
//        for (int i = position; i < position + (resultData.length - position) / 2; i++) {
//            dataU.put(resultData[i]);
//        }
        dataU.put(resultData, position, (resultData.length - position) / 2);

        position = position + (resultData.length - position) / 2;
        dataV.clear();
//        for (int i = position; i < resultData.length; i++) {
//            dataV.put(resultData[i]);
//        }
        dataV.put(resultData, position, resultData.length - position);

        this.videoSink.onFrame(new VideoFrame(i420Buffer, videoFrame.getRotation(), videoFrame.getTimestampNs()));
    }
}
