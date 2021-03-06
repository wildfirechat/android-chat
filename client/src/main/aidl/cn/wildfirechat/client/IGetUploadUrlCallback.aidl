// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

interface IGetUploadUrlCallback {
    void onSuccess(in String uploadUrl, in String remoteUrl, in String backupUploadUrl, in int type);
    void onFailure(in int errorCode);
}
