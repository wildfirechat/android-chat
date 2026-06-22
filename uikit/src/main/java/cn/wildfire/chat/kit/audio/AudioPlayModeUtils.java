/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;

/**
 * 语音消息的播放方式（扬声器/听筒）。该设置是全局的，并持久化保存，重启后保持生效。
 */
public class AudioPlayModeUtils {
    private static final String SP_FILE = "app_settings";
    private static final String EARPIECE_MODE_PREF = "audio_play_in_earpiece";

    private AudioPlayModeUtils() {
    }

    /**
     * @return true 表示使用听筒播放，false 表示使用扬声器播放（默认）。
     */
    public static boolean isEarpieceMode(Context context) {
        return context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE)
            .getBoolean(EARPIECE_MODE_PREF, false);
    }

    public static void setEarpieceMode(Context context, boolean earpiece) {
        SharedPreferences sp = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
        sp.edit().putBoolean(EARPIECE_MODE_PREF, earpiece).apply();
    }

    /**
     * 是否已连接耳机（有线/USB/蓝牙）。连接耳机时声音会走耳机，无需提示“贴近手机聆听”，也不应触发距离传感器息屏。
     */
    public static boolean isHeadsetOn(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return false;
        }
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            switch (device.getType()) {
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_USB_HEADSET:
                case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }
}
