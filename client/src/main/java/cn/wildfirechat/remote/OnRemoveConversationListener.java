/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import cn.wildfirechat.model.Conversation;

public interface OnRemoveConversationListener {
    void onConversationRemove(Conversation conversation);
}
