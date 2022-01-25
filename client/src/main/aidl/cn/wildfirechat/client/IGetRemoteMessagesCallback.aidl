// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.message.Message;

interface IGetRemoteMessagesCallback {
    void onSuccess(in List<Message> messages, in boolean hasMore);
    void onFailure(in int errorCode);
}
