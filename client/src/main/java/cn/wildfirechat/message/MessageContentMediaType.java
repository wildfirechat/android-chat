/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

/**
 * 消息内容媒体类型枚举
 * <p>
 * 定义消息内容的媒体类型。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public enum MessageContentMediaType {
    /**
     * 通用类型
     */
    GENERAL(0),
    /**
     * 图片
     */
    IMAGE(1),
    /**
     * 语音
     */
    VOICE(2),
    /**
     * 视频
     */
    VIDEO(3),
    /**
     * 文件
     */
    FILE(4),
    /**
     * 头像
     */
    PORTRAIT(5),
    /**
     * 收藏
     */
    FAVORITE(6),
    /**
     * 表情
     */
    STICKER(7),
    /**
     * 朋友圈
     */
    MOMENTS(8),
    /**
     * 为客户扩展预留的类型1
     */
    CUSTOM1(9),
    /**
     * 为客户扩展预留的类型2
     */
    CUSTOM2(10),
    /**
     * 为客户扩展预留的类型3
     */
    CUSTOM3(11);

    private int value;

    MessageContentMediaType(int value) {
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
     * 根据值获取媒体类型
     *
     * @param mediaType 媒体类型值
     * @return 媒体类型枚举
     */
    public static MessageContentMediaType mediaType(int mediaType) {
        if (mediaType >= 0 && mediaType < MessageContentMediaType.values().length) {
            return MessageContentMediaType.values()[mediaType];
        }
        return MessageContentMediaType.GENERAL;
        //throw new IllegalArgumentException("mediaType " + mediaType + " is invalid");
    }
}
