package cn.wildfire.chat.net.base;

/**
 * Created by jiangecho on 2017/12/15.
 */

public class ResultWrapper<T> extends StatusResult {
    T result;

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
