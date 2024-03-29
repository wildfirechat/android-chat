// IConnectionStatusChanged.aidl
package cn.chatme.client;


interface IGeneralCallbackInt {
    void onSuccess(in int length);
    void onFailure(in int errorCode);
}
