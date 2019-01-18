package cn.wildfirechat.remote;

import cn.wildfirechat.model.ConversationInfo;

public interface OnUnreadStatusChangeListener {
    /**
     * @param conversationInfo
     * @param clear            表示清除未读状态
     */
    void onUnreadStatusChange(ConversationInfo conversationInfo, boolean clear);
}
