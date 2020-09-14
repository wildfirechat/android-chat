/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import cn.wildfirechat.message.Message;

public interface OnMessageUpdateListener {
    /**
     * messageContent 更新
     *
     * @param messageid
     * @param content
     */
    void onMessageUpdate(Message message);
}
