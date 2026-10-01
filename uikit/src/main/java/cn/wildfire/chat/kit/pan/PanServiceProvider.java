/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import cn.wildfire.chat.kit.Config;

/**
 * 网盘服务提供者。应用层在 {@code Config.isPanConfigured()} 为真时注册实现。
 * <p>
 * UI 一律通过 {@link #isAvailable()} 判断网盘相关入口/菜单是否渲染，未配置时不出现。
 */
public class PanServiceProvider {
    private static PanServiceProvider instance;
    private PanService service;

    private PanServiceProvider() {
    }

    public static synchronized PanServiceProvider getInstance() {
        if (instance == null) {
            instance = new PanServiceProvider();
        }
        return instance;
    }

    public void setService(PanService service) {
        this.service = service;
    }

    public PanService getService() {
        return service;
    }

    /**
     * 服务实现已注册且 {@code PAN_SERVER_ADDRESS} 已配置时才算可用。
     */
    public boolean isAvailable() {
        return service != null && Config.isPanConfigured();
    }
}
