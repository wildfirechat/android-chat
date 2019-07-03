package cn.wildfirechat.remote;

import cn.wildfirechat.model.Conversation;

public interface OnRemoveConversationListener {
    void onConversationRemove(Conversation conversation);
}
