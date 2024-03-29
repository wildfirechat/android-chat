// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.model.ConversationInfo;

interface IGetConversationListCallback {
    void onSuccess(in List<ConversationInfo> infos, in boolean hasMore);
    void onFailure(in int errorCode);
}
