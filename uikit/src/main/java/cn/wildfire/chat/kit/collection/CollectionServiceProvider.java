/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.collection;

/**
 * 接龙服务提供者
 * <p>
 * 用于提供CollectionService实例，应用层需要设置具体的实现。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class CollectionServiceProvider {
    private static CollectionServiceProvider instance;
    private CollectionService service;

    private CollectionServiceProvider() {
    }

    public static synchronized CollectionServiceProvider getInstance() {
        if (instance == null) {
            instance = new CollectionServiceProvider();
        }
        return instance;
    }

    /**
     * 设置接龙服务实现
     *
     * @param service 服务实现
     */
    public void setService(CollectionService service) {
        this.service = service;
    }

    /**
     * 获取接龙服务
     *
     * @return 服务实现，未设置时返回null
     */
    public CollectionService getService() {
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
