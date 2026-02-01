/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.core;

/**
 * 消息持久化标志枚举
 * <p>
 * 定义消息的持久化行为。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public enum PersistFlag {
    /**
     * 消息在本地不存储
     */
    No_Persist(0),

    /**
     * 消息在本地存储
     */
    Persist(1),

    /**
     * 消息在本地存储且计入未读数
     */
    Persist_And_Count(3),

    /**
     * 消息在服务器透传，如果对方不在线会丢弃，不会同步到发送方的多端，不支持聊天室
     */
    Transparent(4);

    private int value;

    PersistFlag(int value) {
        this.value = value;
    }

    /**
     * 获取枚举值
     *
     * @return 枚举值
     */
    public int getValue() {
        return value;
    }

    /**
     * 根据值获取持久化标志
     *
     * @param flag 持久化标志值
     * @return 持久化标志枚举
     */
    public static PersistFlag flag(int flag) {
        PersistFlag pflag = null;
        switch (flag) {
            case 0:
                pflag = No_Persist;
                break;
            case 1:
                pflag = Persist;
                break;
            case 3:
                pflag = Persist_And_Count;
                break;
            case 4:
                pflag = Transparent;
                break;
            default:
                throw new IllegalArgumentException("flag" + flag + " is invalid");
        }
        return pflag;
    }
}
