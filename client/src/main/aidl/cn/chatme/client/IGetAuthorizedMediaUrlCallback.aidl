// IConnectionStatusChanged.aidl
package cn.chatme.client;

interface IGetAuthorizedMediaUrlCallback {
    void onSuccess(in String url, in String backupUrl);
    void onFailure(in int errorCode);
}
