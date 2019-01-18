// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

interface IGeneralCallback {
    void onSuccess();
    void onFailure(in int errorCode);
}
