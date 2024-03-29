// IConnectionStatusChanged.aidl
package cn.chatme.client;



interface ISendMessageCallback {
    void onSuccess(long messageUid, long timestamp);
    void onFailure(int errorCode);
    void onPrepared(long messageId, long savedTime);
    void onProgress(long uploaded, long total);
    void onMediaUploaded(String remoteUrl);
}
