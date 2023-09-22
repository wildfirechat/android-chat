/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import io.kvh.media.amr.AmrDecoder;
import io.kvh.media.amr.AmrEncoder;

// FYI http://www.isilent.me/551.html
public class AmrVolumeLouder {
    private static final int[] amrEncodeMode = {4750, 5150, 5900, 6700, 7400, 7950, 10200, 12200}; // amr 编码方式
    private static final int AMR_FRAME_COUNT_PER_SECOND = 50;
    private static int mMode = 0;

    public static boolean increaseVolume(String inPath, String outPath, int factor) {
        File inFile = new File(inPath);
        File outFile = new File(outPath);
        if (!inFile.exists()) {
            return false;
        }
        boolean result = false;
        try (
            FileInputStream in = new FileInputStream(inFile);
            FileOutputStream out = new FileOutputStream(outFile)
        ) {
            outFile.createNewFile();

            byte[] amrHead = new byte[6];// amr head 6 bytes

            in.read(amrHead);
            out.write(amrHead);

            byte[] amrFrameHead = new byte[1];// amr frame head 1 bytes
            in.read(amrFrameHead);
            out.write(amrFrameHead);

            int frameSize = calcAMRFrameSize(amrFrameHead[0]);
            byte[] amrFrameContent = new byte[frameSize - 1];// amr frame content frameSize - 1 bytes
            in.read(amrFrameContent);
            out.write(amrFrameContent);

            byte[] amrFrame = new byte[frameSize];// amr frame frameSize bytes
            short[] pcmFrame = new short[160];// pcm frame 160 shorts

            long state = AmrDecoder.init();
            AmrEncoder.init(0);

            while (in.read(amrFrame) != -1) {
                AmrDecoder.decode(state, amrFrame, pcmFrame);
                int newpcmVal;
                for (int i = 0; i < pcmFrame.length; i++) {
                    // 音量调节
                    newpcmVal = pcmFrame[i] * factor;
                    if (newpcmVal < 32767 && newpcmVal > -32768) {
                        pcmFrame[i] = (short) newpcmVal;
                    } else if (newpcmVal > 32767) {
                        pcmFrame[i] = 32767;
                    } else {
                        pcmFrame[i] = -32768;
                    }
                }
                AmrEncoder.encode(mMode, pcmFrame, amrFrame);
                out.write(amrFrame);
            }
            out.flush();
            result = true;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            AmrEncoder.exit();
        }
        return result;
    }

    // 根据帧头计算当前帧大小
    private static int calcAMRFrameSize(byte frameHeader) {
        int mode;
        int temp2 = 0;
        int frameSize;
        mMode = (frameHeader & 0x78) >> 3; // 编码方式编号 = 帧头的3-6位 temp1 &= 0x78; // 0111-1000 temp1 >>= 3;

        mode = amrEncodeMode[mMode];

        // 计算amr音频数据帧大小
        // 原理: amr 一帧对应20ms，那么一秒有50帧的音频数据
        temp2 = round((double) (((double) mode / (double) AMR_FRAME_COUNT_PER_SECOND) / (double) 8));
        frameSize = round((double) temp2 + 0.5);
        return frameSize;
    }

    private static int round(double x) {
        return ((int) (x + 0.5));
    }
}
