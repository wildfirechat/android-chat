package cn.wildfire.chat.kit.net.base;

/**
 * Created by imndx on 2017/12/15.
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
