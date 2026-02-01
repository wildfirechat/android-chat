/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

/**
 * 空聊天室信息类
 * <p>
 * 当聊天室信息不存在时返回的空对象实现。
 * 使用Null Object模式，避免上层代码不断的做空值检查。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class NullChatRoomInfo extends ChatRoomInfo {
    public NullChatRoomInfo(String chatRoomId) {
        this.chatRoomId = chatRoomId;
        //this.title = "<" + chatRoomId + ">";
        this.title = "聊天室";
    }
}