/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.archive.service;

/**
 * 归档服务提供者
 * <p>
 * 用于提供 ArchiveService 实例，应用层需要设置具体的实现。
 * </p>
 */
public class ArchiveServiceProvider {
    private static ArchiveServiceProvider instance;
    private ArchiveService service;

    private ArchiveServiceProvider() {
    }

    public static synchronized ArchiveServiceProvider getInstance() {
        if (instance == null) {
            instance = new ArchiveServiceProvider();
        }
        return instance;
    }

    /**
     * 设置归档服务实现
     *
     * @param service 服务实现
     */
    public void setService(ArchiveService service) {
        this.service = service;
    }

    /**
     * 获取归档服务
     *
     * @return 服务实现，未设置时返回 null
     */
    public ArchiveService getService() {
        return service;
    }

    /**
     * 检查服务是否可用
     *
     * @return true=可用
     */
    public boolean isAvailable() {
        return service != null;
    }
}
