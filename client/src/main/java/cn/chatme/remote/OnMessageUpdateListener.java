/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.message.Message;

public interface OnMessageUpdateListener {
    /**
     * messageContent 更新
     *
     * @param messageid
     * @param content
     */
    void onMessageUpdate(Message message);
}
