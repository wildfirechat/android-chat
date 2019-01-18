package cn.wildfirechat.remote;

public interface GeneralCallback {
    void onSuccess();

    void onFailure(int errorCode);
}
