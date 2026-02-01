/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.core;

/**
 * 消息方向枚举
 * <p>
 * 定义消息是发送还是接收。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public enum MessageDirection {
    /**
     * 发送消息
     */
    Send(0),
    /**
     * 接收消息
     */
    Receive(1);

    private int value;

    MessageDirection(int value) {
        this.value = value;
    }

    /**
     * 获取枚举值
     *
     * @return 枚举值
     */
    public int value() {
        return this.value;
    }

    /**
     * 根据值获取消息方向
     *
     * @param direction 消息方向值
     * @return 消息方向枚举
     */
    public static MessageDirection direction(int direction) {
        MessageDirection messageDirection = null;
        switch (direction) {
            case 0:
                messageDirection = Send;
                break;
            case 1:
                messageDirection = Receive;
                break;
            default:
                throw new IllegalArgumentException("direction " + direction + " is invalid");
        }
        return messageDirection;

    }
}
