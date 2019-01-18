package cn.wildfirechat.remote;

public interface SendMessageCallback {
    void onSuccess(long messageUid, long timestamp);

    void onFailure(int errorCode);

    void onPrepared(long messageId, long savedTime);

    default void onProgress(long uploaded, long total) {
    }


    default void onMediaUploaded(String remoteUrl) {
    }

}
