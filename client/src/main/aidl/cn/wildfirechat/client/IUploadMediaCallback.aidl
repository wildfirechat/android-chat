// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;



interface IUploadMediaCallback {
    void onSuccess(in String remoteUrl);
    void onProgress(long uploaded, long total);
    void onFailure(in int errorCode);
}
