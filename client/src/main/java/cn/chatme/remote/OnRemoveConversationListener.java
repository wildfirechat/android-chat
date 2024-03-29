/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.model.Conversation;

public interface OnRemoveConversationListener {
    void onConversationRemove(Conversation conversation);
}
