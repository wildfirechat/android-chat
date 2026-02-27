/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.pan;

import cn.wildfire.chat.kit.pan.api.PanService;

/**
 * 网盘服务提供者
 */
public class PanServiceProvider {
    
    /**
     * 获取网盘服务实例
     */
    public static PanService getPanService() {
        return PanServiceImpl.getInstance();
    }
    
    /**
     * 初始化网盘服务
     * @param baseUrl 网盘服务基础URL
     */
    public static void init(String baseUrl) {
        PanServiceImpl.getInstance().setBaseUrl(baseUrl);
    }
}
