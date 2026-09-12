/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import io.kvh.media.amr.AmrEncoder;

/**
 * 把 16kHz、16-bit、单声道 PCM 编码成 AMR-NB 文件，格式和 {@link AudioRecorder} 录制的语音消息一致
 * <p>
 * AmrEncoder 是全局唯一的编码器，请在 ChatManager 的 work handler 中调用，与 {@link AmrVolumeLouder} 串行
 */
public class PcmAmrEncoder {
    private static final String TAG = "PcmAmrEncoder";
    private static final byte[] AMR_HEADER = "#!AMR\n".getBytes(StandardCharsets.US_ASCII);
    // 12.2kbps，与 AudioRecorder 录音的码率一致
    private static final int AMR_MODE_MR122 = 7;
    // AMR-NB 每帧 20ms，8kHz 下是 160 个采样
    private static final int FRAME_SAMPLES = 160;
    // 16kHz 降采样到 8kHz 前的低通滤波器，滤掉 4kHz 以上的声音，避免混叠
    private static final double[] LOW_PASS_FILTER = createHalfBandFilter(31);

    /**
     * @param pcm     16kHz、16-bit、小端、单声道 PCM
     * @param length  pcm 的有效字节数
     * @param outPath 输出的 AMR 文件
     * @param gain    音量放大倍数，1 表示不放大
     * @return 是否编码成功
     */
    public static boolean encode(@NonNull byte[] pcm, int length, @NonNull String outPath, int gain) {
        int inSamples = length / 2;
        int halfTaps = LOW_PASS_FILTER.length / 2;
        short[] frame = new short[FRAME_SAMPLES];
        byte[] encoded = new byte[64];
        int frameSamples = 0;
        try (FileOutputStream out = new FileOutputStream(outPath)) {
            AmrEncoder.init(0);
            out.write(AMR_HEADER);
            // 滤波后每两个采样取一个，降到 8kHz
            for (int i = 0; i < inSamples; i += 2) {
                double sum = 0;
                for (int k = 0; k < LOW_PASS_FILTER.length; k++) {
                    int index = Math.min(Math.max(i + k - halfTaps, 0), inSamples - 1);
                    sum += LOW_PASS_FILTER[k] * (short) ((pcm[2 * index] & 0xff) | (pcm[2 * index + 1] << 8));
                }
                frame[frameSamples++] = clamp(sum * gain);
                if (frameSamples == FRAME_SAMPLES) {
                    writeFrame(out, frame, encoded);
                    frameSamples = 0;
                }
            }
            if (frameSamples > 0) {
                // 最后不足一帧的部分补静音
                Arrays.fill(frame, frameSamples, FRAME_SAMPLES, (short) 0);
                writeFrame(out, frame, encoded);
            }
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "编码 AMR 失败", e);
            return false;
        } finally {
            AmrEncoder.exit();
        }
    }

    private static void writeFrame(@NonNull FileOutputStream out, @NonNull short[] frame, @NonNull byte[] encoded) throws IOException {
        int size = AmrEncoder.encode(AMR_MODE_MR122, frame, encoded);
        if (size > 0) {
            out.write(encoded, 0, size);
        }
    }

    private static short clamp(double value) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(value)));
    }

    /**
     * 截止频率为采样率 1/4 的加窗（Hamming）半带低通滤波器
     */
    @NonNull
    private static double[] createHalfBandFilter(int taps) {
        double[] filter = new double[taps];
        int center = taps / 2;
        double sum = 0;
        for (int i = 0; i < taps; i++) {
            int n = i - center;
            double sinc = n == 0 ? 0.5 : Math.sin(Math.PI * n / 2) / (Math.PI * n);
            double window = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (taps - 1));
            filter[i] = sinc * window;
            sum += filter[i];
        }
        for (int i = 0; i < taps; i++) {
            filter[i] /= sum;
        }
        return filter;
    }
}
