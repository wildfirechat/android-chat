/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

/**
 * 用户ID名称头像类
 * <p>
 * 用于表示用户的基本信息：ID、名称和头像。
 * 常用于轻量级的用户信息传递。
 * </p>
 *
 * @author WildFireChat
 * @since 2025
 */
public class UserIdNamePortrait {
    /**
     * 用户ID
     */
    public String userId;

    /**
     * 用户名称
     */
    public String name;

    /**
     * 用户头像URL
     */
    public String portrait;

}
