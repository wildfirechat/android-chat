package cn.wildfirechat.remote;

public interface GeneralCallback {
    void onSuccess();

    void onFail(int errorCode);
}
