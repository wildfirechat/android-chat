package cn.wildfirechat.remote;

public interface SendMessageCallback {
    void onSuccess(long messageUid, long timestamp);

    void onFail(int errorCode);

    void onPrepare(long messageId, long savedTime);

    default void onProgress(long uploaded, long total) {
    }


    default void onMediaUpload(String remoteUrl) {
    }

}
