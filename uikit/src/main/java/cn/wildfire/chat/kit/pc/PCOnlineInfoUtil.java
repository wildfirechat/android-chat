/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pc;

import android.content.Context;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.client.Platform;
import cn.wildfirechat.model.PCOnlineInfo;

/**
 * 多端在线信息展示辅助：平台图标、设备名称、是否桌面电脑。
 * 多端登录条（会话列表）与「已登录的设备」页共用，保证图标/命名一致，
 * 与 HarmonyOS 版的 {@code pcOnlineUtil.ets} 一一对应。
 * <p>
 * 注意：PCOnlineInfo 的 type（PC/Web/WX/Pad 在线类型）由服务端权威下发，
 * 而 platform 是各端上报的具体平台号，老客户端可能上报 0（UNSET），
 * 因此图标/命名以 type 为主、platform 为辅，避免 platform 缺失时全部显示成手机。
 */
public class PCOnlineInfoUtil {

    /**
     * 平台灰色图标。
     * 桌面电脑类（Windows/OSX/Linux/HarmonyPC）→ 电脑；WEB → 网站；
     * WX → 手机；iPad/APad/HarmonyPad → 平板；其余未知平台兜底为手机。
     */
    public static int platformIconRes(PCOnlineInfo info) {
        if (info != null && info.getType() != null) {
            switch (info.getType()) {
                case PC_Online:
                    return R.drawable.ic_pc_computer;
                case Web_Online:
                    return R.drawable.ic_web_globe;
                case Pad_Online:
                    return R.drawable.ic_pad;
                case WX_Online:
                    return R.drawable.ic_phone;
            }
        }
        if (info != null && info.getPlatform() != null) {
            switch (info.getPlatform()) {
                case PlatformType_Windows:
                case PlatformType_OSX:
                case PlatformType_Linux:
                case PlatformType_Harmony_PC:
                    return R.drawable.ic_pc_computer;
                case PlatformType_WEB:
                    return R.drawable.ic_web_globe;
                case PlatformType_WX:
                    return R.drawable.ic_phone;
                case PlatformType_iPad:
                case PlatformType_APad:
                case PlatformType_Harmony_Pad:
                    return R.drawable.ic_pad;
                default:
                    break;
            }
        }
        return R.drawable.ic_phone;
    }

    /**
     * 设备名称（微信风格）：Windows / Mac / Linux / 鸿蒙PC / Web / 小程序 /
     * iPad / Android 平板 / 鸿蒙Pad 等。type 特化的（小程序/手表/电视）优先，
     * 否则用平台上报的名字；平台缺失（UNSET 返回 "PC"）时按在线类型回退。
     */
    public static String deviceName(Context context, PCOnlineInfo info) {
        PCOnlineInfo.PCOnlineType type = info == null ? null : info.getType();
        if (type != null) {
            switch (type) {
                case WX_Online:
                    return context.getString(R.string.pc_device_mini_program);
                default:
                    break;
            }
        }
        String name = info != null && info.getPlatform() != null ? info.getPlatform().getPlatFormName() : "PC";
        if ("PC".equals(name)) {
            // platform 未上报（UNSET）时按在线类型回退
            if (type == PCOnlineInfo.PCOnlineType.Web_Online) {
                return context.getString(R.string.pc_device_web);
            }
            if (type == PCOnlineInfo.PCOnlineType.Pad_Online) {
                return context.getString(R.string.pc_device_pad);
            }
            return context.getString(R.string.pc_device_computer);
        }
        return name;
    }

    /**
     * 是否为桌面电脑类设备（支持「锁定电脑」等操作）。
     */
    public static boolean isDesktopDevice(PCOnlineInfo info) {
        if (info == null) {
            return false;
        }
        if (info.getType() == PCOnlineInfo.PCOnlineType.PC_Online) {
            return true;
        }
        Platform platform = info.getPlatform();
        return platform == Platform.PlatformType_Windows
            || platform == Platform.PlatformType_OSX
            || platform == Platform.PlatformType_Linux
            || platform == Platform.PlatformType_Harmony_PC;
    }

    private PCOnlineInfoUtil() {
    }
}
