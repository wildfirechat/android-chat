package cn.wildfire.chat.kit.net;

public interface BooleanCallback {
    void onSuccess(boolean result);

    void onFail(int code, String msg);
}
