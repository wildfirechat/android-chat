// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.message.Message;

interface IGetRemoteMessageCallback {
    void onSuccess(in List<Message> messages);
    void onFailure(in int errorCode);
}
