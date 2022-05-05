// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;


interface IGeneralCallbackInt {
    void onSuccess(in int length);
    void onFailure(in int errorCode);
}
