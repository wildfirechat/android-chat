// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.Message;
interface IGetRemoteMessageCallback {
    void onSuccess(in List<Message> messages);
    void onFailure(in int errorCode);
}
