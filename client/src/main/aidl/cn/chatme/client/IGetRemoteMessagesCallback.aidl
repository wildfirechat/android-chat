// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.message.Message;

interface IGetRemoteMessagesCallback {
    void onSuccess(in List<Message> messages, in boolean hasMore);
    void onFailure(in int errorCode);
}
