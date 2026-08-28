/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 语音消息的播放方式（扬声器/听筒）。该设置是全局的，并持久化保存，重启后保持生效。
 * <p>
 * 有两个入口能改它：设置 → 聊天里的开关，和语音消息长按菜单里的「听筒/扬声器播放」。
 * 两个入口读写的是同一份数据，改动通过 {@link OnAudioPlayModeChangedListener} 广播出去，
 * 于是另一个入口以及会话页标题上的听筒图标都会立刻跟着变。
 */
public class AudioPlayModeUtils {
    private static final String SP_FILE = "app_settings";
    private static final String EARPIECE_MODE_PREF = "audio_play_in_earpiece";

    private static final List<OnAudioPlayModeChangedListener> listeners = new CopyOnWriteArrayList<>();

    private AudioPlayModeUtils() {
    }

    /**
     * 播放方式变化的观察者。注册之后记得在页面销毁时
     * {@link #removeOnAudioPlayModeChangedListener(OnAudioPlayModeChangedListener)}，否则会泄漏。
     */
    public interface OnAudioPlayModeChangedListener {
        /**
         * @param earpiece 变化之后的播放方式，true 为听筒
         */
        void onAudioPlayModeChanged(boolean earpiece);
    }

    /**
     * @return true 表示使用听筒播放，false 表示使用扬声器播放（默认）。
     */
    public static boolean isEarpieceMode(Context context) {
        return context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE)
            .getBoolean(EARPIECE_MODE_PREF, false);
    }

    public static void setEarpieceMode(Context context, boolean earpiece) {
        if (isEarpieceMode(context) == earpiece) {
            // 值没变就不广播，免得开关回填时和监听者绕成一个圈
            return;
        }
        SharedPreferences sp = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
        sp.edit().putBoolean(EARPIECE_MODE_PREF, earpiece).apply();
        for (OnAudioPlayModeChangedListener listener : listeners) {
            listener.onAudioPlayModeChanged(earpiece);
        }
    }

    public static void addOnAudioPlayModeChangedListener(OnAudioPlayModeChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeOnAudioPlayModeChangedListener(OnAudioPlayModeChangedListener listener) {
        listeners.remove(listener);
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
