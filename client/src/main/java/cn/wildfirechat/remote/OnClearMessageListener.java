package cn.wildfirechat.remote;

import cn.wildfirechat.model.Conversation;

/**
 * 会话消息被清空回调
 */
public interface OnClearMessageListener {
    void onClearMessage(Conversation conversation);
}
