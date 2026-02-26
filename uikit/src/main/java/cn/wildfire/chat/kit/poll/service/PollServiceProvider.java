/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.service;

/**
 * 投票服务提供者
 * <p>
 * 用于提供 PollService 实例，应用层需要设置具体的实现。
 * </p>
 */
public class PollServiceProvider {
    private static PollServiceProvider instance;
    private PollService service;

    private PollServiceProvider() {
    }

    public static synchronized PollServiceProvider getInstance() {
        if (instance == null) {
            instance = new PollServiceProvider();
        }
        return instance;
    }

    /**
     * 设置投票服务实现
     *
     * @param service 服务实现
     */
    public void setService(PollService service) {
        this.service = service;
    }

    /**
     * 获取投票服务
     *
     * @return 服务实现，未设置时返回 null
     */
    public PollService getService() {
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
