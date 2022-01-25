// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.ConversationInfo;

interface IGetConversationListCallback {
    void onSuccess(in List<ConversationInfo> infos, in boolean hasMore);
    void onFailure(in int errorCode);
}
