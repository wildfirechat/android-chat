// IConnectionStatusChanged.aidl
package cn.chatme.client;

interface IGeneralCallback2 {
    void onSuccess(in String success);
    void onFailure(in int errorCode);
}
