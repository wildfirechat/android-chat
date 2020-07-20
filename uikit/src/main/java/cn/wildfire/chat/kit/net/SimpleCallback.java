package cn.wildfire.chat.kit.net;


import android.os.Handler;
import android.os.Looper;

/**
 * Created by imndx on 2017/12/15.
 */

public abstract class SimpleCallback<T> implements Callback<T> {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onSuccess(final T t) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onUiSuccess(t);
            }
        });
    }

    @Override
    public void onFailure(final int code, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onUiFailure(code, message);
            }
        });
    }

    public abstract void onUiSuccess(T t);

    public abstract void onUiFailure(int code, String msg);
}
