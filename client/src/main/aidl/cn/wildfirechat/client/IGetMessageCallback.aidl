// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.message.Message;

interface IGetMessageCallback {
    void onSuccess(in List<Message> messages, in boolean hasMore);
    void onFailure(in int errorCode);
}
