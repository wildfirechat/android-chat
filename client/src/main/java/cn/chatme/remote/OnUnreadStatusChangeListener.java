/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.model.ConversationInfo;

public interface OnUnreadStatusChangeListener {
    /**
     * @param conversationInfo
     * @param clear            表示清除未读状态
     */
    void onUnreadStatusChange(ConversationInfo conversationInfo, boolean clear);
}
