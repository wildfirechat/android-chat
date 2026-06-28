/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import android.text.TextUtils;

import cn.wildfire.chat.kit.Config;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UrlRedirector;

/**
 * 网络地址转换参考实现
 */
public class WFUrlRedirector implements UrlRedirector {
    /**
     * 双网时，需要根据网络类型进行地址转换
     *
     * @param originalUrl 原始 url
     * @return 根据网络环境转换之后的 url
     */
    @Override
    public String urlRedirect(String originalUrl) {
        if (TextUtils.isEmpty(Config.MAIN_MEDIA_URL_PREFIX) || TextUtils.isEmpty(Config.BACKUP_MEDIA_URL_PREFIX)) {
            return originalUrl;
        }

        if (ChatManager.Instance().isConnectedToMainNetwork()) {
            // 当前连接的是主网络，把备网媒体前缀替换成主网的
            return originalUrl.replace(Config.BACKUP_MEDIA_URL_PREFIX, Config.MAIN_MEDIA_URL_PREFIX);
        } else {
            // 当前连接的是备选网络，把主网媒体前缀替换成备网的
            return originalUrl.replace(Config.MAIN_MEDIA_URL_PREFIX, Config.BACKUP_MEDIA_URL_PREFIX);
        }
    }
}
