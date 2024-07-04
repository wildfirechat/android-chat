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
    String urlRedirect(String originalUrl);
}
