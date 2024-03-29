/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.message.core.PersistFlag;
import cn.chatme.message.Message;


/**
 * * 当消息为{@link PersistFlag#No_Persist}也进行通知，当不需要是，需要自行处理
 */
public interface OnReceiveMessageListener {
    void onReceiveMessage(List<Message> messages, boolean hasMore);
}
