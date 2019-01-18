// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;



interface ISendMessageCallback {
    void onSuccess(long messageId, long timestamp);
    void onFailure(int errorCode);
    void onPrepared(long messageId, long savedTime);
    void onProgress(long uploaded, long total);
    void onMediaUploaded(String remoteUrl);
}
