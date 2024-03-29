// IConnectionStatusChanged.aidl
package cn.chatme.client;



interface IUploadMediaCallback {
    void onSuccess(in String remoteUrl);
    void onProgress(long uploaded, long total);
    void onFailure(in int errorCode);
}
