/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

/**
 * 双网环境时使用
 * <p>
 * 双网环境时，媒体内容 url 转换器
 */
public interface UrlRedirector {
    /**
     * 双网时，需要根据网络类型进行地址转换
     *
     * @param originalUrl 原始 url
     * @return 根据网络环境转换之后的 url
     */
    String urlRedirect(String originalUrl);
}
