package cn.wildfire.chat.kit.net.base;

/**
 * Created by imndx on 2017/12/16.
 */

/**
 * 用来表示result的状态，上层基本不用关注
 */
public class StatusResult {
    private int code;
    private String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return code == 0;
    }
}
