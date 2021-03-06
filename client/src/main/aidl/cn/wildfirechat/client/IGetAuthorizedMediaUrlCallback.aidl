// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

interface IGetAuthorizedMediaUrlCallback {
    void onSuccess(in String url, in String backupUrl);
    void onFailure(in int errorCode);
}
