package cn.wildfirechat.remote;

import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.UnreadCount;

public interface OnConversationInfoUpdateListener {

    void onConversationDraftUpdate(ConversationInfo conversationInfo, String draft);

    void onConversationTopUpdate(ConversationInfo conversationInfo, boolean top);

    void onConversationSilentUpdate(ConversationInfo conversationInfo, boolean silent);

    /**
     * @param conversationInfo
     * @param originalUnread   原始未读状态
     */
    void onConversationUnreadStatusClear(ConversationInfo conversationInfo, UnreadCount originalUnread);

    // 可能是receive、send、recall 触发
    // 有个问题，未读消息基数做不了，还是得send、receive、recall三个回调来做
    //void onConversationLatestMessageUpdate(ConversationInfo conversationInfo, Message message);
}
