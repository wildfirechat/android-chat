/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.model.Conversation;

/**
 * 会话消息被清空回调
 */
public interface OnClearMessageListener {
    void onClearMessage(Conversation conversation);
}
