/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import cn.wildfirechat.remote.UrlRedirector;

/**
 * Network address redirection reference implementation
 */
public class TestUrlRedirector implements UrlRedirector {
    /**
     * For dual network, address redirection needs to be performed based on network type
     *
     * @param originalUrl original url
     * @return url after redirection based on network environment
     */
    @Override
    public String urlRedirect(String originalUrl) {
        // No dual network environment deployed, so return directly
        return originalUrl;

        // For dual network environment, please refer to the reference implementation below
//        if (ChatManager.Instance().isConnectedToMainNetwork()) {
//            // Currently connected to main network
//            originalUrl = originalUrl.replace("192.168.2.19", "oss.xxxx.com");
//        } else {
//            // Currently connected to backup network
//            originalUrl = originalUrl.replace("oss.xxxx.com", "192.168.2.19");
//        }
//        return originalUrl;
    }
}
