/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.asr;

import android.net.Uri;

import androidx.annotation.NonNull;

import cn.wildfire.chat.kit.Config;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;

/**
 * asr-api 鉴权
 * <p>
 * 请求 asr-api 时，需要在 HTTP header authCode 中带上从 IM 服务获取的认证码，asr-api 向 IM 服务校验认证码后得到用户 ID。
 * 认证码 1 分钟内有效，每次请求前重新获取。
 */
public class AsrAuth {
    public static final String HEADER_AUTH_CODE = "authCode";

    // 和组织通讯录服务一样，使用管理后台类型（ApplicationType_Admin）的认证码
    private static final String AUTH_CODE_APP_ID = "admin";
    private static final int AUTH_CODE_TYPE = 2;

    /**
     * 是否是 asr-api 的地址。asr-api 的接口都在 /api/ 路径下，直连 wf-voice 的地址没有路径
     */
    public static boolean isAsrApiUrl(@NonNull String url) {
        String path = Uri.parse(url).getPath();
        return path != null && path.contains("/api/");
    }

    /**
     * 获取认证码，在主线程回调
     */
    public static void getAuthCode(@NonNull GeneralCallback2 callback) {
        ChatManager.Instance().getAuthCode(AUTH_CODE_APP_ID, AUTH_CODE_TYPE, Config.IM_SERVER_HOST, callback);
    }
}
