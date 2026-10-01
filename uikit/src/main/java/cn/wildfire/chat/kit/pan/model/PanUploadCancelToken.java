/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 上传取消令牌。上传过程中轮询 {@link #isCancelled()}，取消后回调以 code -2 结束。
 */
public class PanUploadCancelToken {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        cancelled.set(true);
    }
}
