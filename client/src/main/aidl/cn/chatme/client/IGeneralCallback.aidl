// IConnectionStatusChanged.aidl
package cn.chatme.client;

interface IGeneralCallback {
    void onSuccess();
    void onFailure(in int errorCode);
}
