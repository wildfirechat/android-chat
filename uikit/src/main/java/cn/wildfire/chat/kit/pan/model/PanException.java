/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

/**
 * 网盘接口异常：服务端返回 {@code code != 0} 时抛出，code 保留原始业务码。
 */
public class PanException extends Exception {
    private final int code;

    public PanException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
