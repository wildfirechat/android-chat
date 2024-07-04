/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import cn.wildfirechat.remote.UrlRedirector;

/**
 * 网络地址转换参考实现
 */
public class TestUrlRedirector implements UrlRedirector {
    /**
     * 双网时，需要根据网络类型进行地址转换
     *
     * @param originalUrl 原始 url
     * @return 根据网络环境转换之后的 url
     */
    @Override
    public String urlRedirect(String originalUrl) {
        // 未部署双网环境，故直接返回
        return originalUrl;

        // 双网环境时，请参考下面的参考实现
//        if (ChatManager.Instance().isConnectedToMainNetwork()) {
//            // 当前连接的是主网络
//            originalUrl = originalUrl.replace("192.168.2.19", "oss.xxxx.com");
//        } else {
//            // 当前连接的是备选网络
//            originalUrl = originalUrl.replace("oss.xxxx.com", "192.168.2.19");
//        }
//        return originalUrl;
    }
}
