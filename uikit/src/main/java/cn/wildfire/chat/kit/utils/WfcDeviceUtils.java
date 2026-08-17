/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import cn.wildfire.chat.kit.R;

/**
 * 平板（Pad）适配相关的设备与窗口判定。
 * <p>
 * 这里有两个<strong>必须严格区分</strong>的判定，混用会出线上问题：
 * <ul>
 * <li>{@link #isTwoPaneLayout(Context)} —— <strong>布局</strong>决策。跟随 {@code values-sw600dp}
 * 资源限定符，分屏拖动、折叠屏开合时<strong>实时变化</strong>，与布局资源天然同步。</li>
 * <li>{@link #isPadDevice(Context)} —— <strong>平台身份</strong>决策（登录时是否上报
 * {@code platform=9/APad}）。首次启动时按设备物理尺寸判定并持久化，之后<strong>恒定不变</strong>。
 * 因为 token 与 platform 强绑定，中途变化会导致连接失败。</li>
 * </ul>
 * 反例：用 {@code isPadDevice()} 决定布局，平板分屏到窄窗口时仍走双栏，布局错乱；
 * 用 {@code isTwoPaneLayout()} 决定登录 platform，用户拖一下分屏就改变了平台身份，token 失效。
 * <p>
 * 手机上两者恒为 {@code false}，所有手机端代码路径不受影响。
 */
public class WfcDeviceUtils {
    private static final String SP_FILE = "app_settings";
    private static final String PAD_DEVICE_PREF = "is_pad_device";

    /**
     * 平板门槛：最小宽度 600dp，与 {@code res/values-sw600dp} 限定符保持一致。
     */
    private static final int PAD_SMALLEST_WIDTH_DP = 600;

    private static volatile Boolean sPadDevice;

    private WfcDeviceUtils() {
    }

    /**
     * 当前<strong>窗口</strong>是否应使用平板双栏布局。
     * <p>
     * 取值来自 {@code R.bool.wfc_two_pane}（默认 false，{@code values-sw600dp} 下为 true），
     * 因此与 {@code -sw600dp} 布局资源永远一致。分屏、折叠屏开合后 Activity 重建，取值随之更新。
     * <p>
     * 必须传入 Activity 等 UI Context；传 Application Context 在多窗口下取值不准。
     */
    public static boolean isTwoPaneLayout(Context context) {
        return context.getResources().getBoolean(R.bool.wfc_two_pane);
    }

    /**
     * 当前<strong>设备</strong>是否为平板。首次调用时判定并持久化，之后恒定返回同一结果。
     * <p>
     * 用于登录平台身份（{@code ChatManager.setPlatform}）、以及是否放开横屏等
     * 「不允许中途变化」的决策。不要用它决定布局，布局请用 {@link #isTwoPaneLayout(Context)}。
     */
    public static boolean isPadDevice(Context context) {
        Boolean cached = sPadDevice;
        if (cached != null) {
            return cached;
        }
        synchronized (WfcDeviceUtils.class) {
            if (sPadDevice != null) {
                return sPadDevice;
            }
            SharedPreferences sp = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
            boolean isPad;
            if (sp.contains(PAD_DEVICE_PREF)) {
                isPad = sp.getBoolean(PAD_DEVICE_PREF, false);
            } else {
                isPad = detectPadDevice(context);
                sp.edit().putBoolean(PAD_DEVICE_PREF, isPad).apply();
            }
            sPadDevice = isPad;
            return isPad;
        }
    }

    /**
     * 是否允许横屏。平板放开，手机维持原有的强制竖屏。
     * <p>
     * 单独提供这个语义方法，是为了将来若要调整策略（例如折叠屏展开态也放开横屏），
     * 只需改这一处，而不用去动 85 个 Activity 的基类判断。
     */
    public static boolean isLandscapeAllowed(Context context) {
        return isPadDevice(context);
    }

    /**
     * 实时检测当前设备是否为平板，<strong>不读取也不写入</strong>持久化结果。
     * <p>
     * 供「持久值与当前设备不符」的校验场景使用（如换机后恢复数据，此时需要清除 token 重新登录，
     * 否则 platform 与 token 不匹配会连不上）。日常判定请用 {@link #isPadDevice(Context)}。
     */
    public static boolean detectPadDevice(Context context) {
        // 优先用系统资源的 Configuration：它就是资源限定符 sw<N>dp 所依据的那个值，
        // 且不受本应用是否处于分屏/自由窗口影响。
        int smallestWidthDp = Resources.getSystem().getConfiguration().smallestScreenWidthDp;
        if (smallestWidthDp <= 0) {
            // 个别 ROM 上该值不可靠，退回按物理屏幕尺寸估算。
            smallestWidthDp = smallestWidthDpFromRealMetrics(context);
        }
        return smallestWidthDp >= PAD_SMALLEST_WIDTH_DP;
    }

    private static int smallestWidthDpFromRealMetrics(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return 0;
        }
        DisplayMetrics dm = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.getDefaultDisplay().getRealMetrics(dm);
        } else {
            wm.getDefaultDisplay().getMetrics(dm);
        }
        if (dm.density <= 0) {
            return 0;
        }
        return (int) (Math.min(dm.widthPixels, dm.heightPixels) / dm.density);
    }
}
