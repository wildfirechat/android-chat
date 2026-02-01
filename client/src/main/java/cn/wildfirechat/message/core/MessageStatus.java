/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.core;

/**
 * 消息状态枚举
 * <p>
 * 定义消息的各种状态，包括发送状态、已读状态等。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public enum MessageStatus {
    /**
     * 发送中
     */
    Sending(0),
    /**
     * 已发送
     */
    Sent(1),
    /**
     * 发送失败
     */
    Send_Failure(2),
    /**
     * 被@提醒
     */
    Mentioned(3),
    /**
     * 被@所有人提醒
     */
    AllMentioned(4),
    /**
     * 未读
     */
    Unread(5),
    /**
     * 已读
     */
    Readed(6),
    /**
     * 已播放（语音/视频消息）
     */
    Played(7),
    /**
     * 所有状态
     */
    All(8);

    private int value;

    MessageStatus(int value) {
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
     * 根据值获取消息状态
     *
     * @param status 消息状态值
     * @return 消息状态枚举
     */
    public static MessageStatus status(int status) {
        MessageStatus messageStatus = null;
        switch (status) {
            case 0:
                messageStatus = Sending;
                break;
            case 1:
                messageStatus = Sent;
                break;
            case 2:
                messageStatus = Send_Failure;
                break;
            case 3:
                messageStatus = Mentioned;
                break;
            case 4:
                messageStatus = AllMentioned;
                break;
            case 5:
                messageStatus = Unread;
                break;
            case 6:
                messageStatus = Readed;
                break;
            case 7:
                messageStatus = Played;
                break;
            default:
                throw new IllegalArgumentException("status " + status + "is not valid");
        }
        return messageStatus;
    }
}
