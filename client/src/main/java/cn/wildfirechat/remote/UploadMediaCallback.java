package cn.wildfirechat.remote;

public interface UploadMediaCallback {
    void onSuccess(String result);

    void onProgress(long uploaded, long total);

    void onFail(int errorCode);
}
