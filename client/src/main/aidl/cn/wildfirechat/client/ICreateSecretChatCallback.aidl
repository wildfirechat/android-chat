// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

interface ICreateSecretChatCallback {
    void onSuccess(in String targetId, in int line);
    void onFailure(in int errorCode);
}
