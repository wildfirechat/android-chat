package cn.wildfire.chat.kit.service;

public class BaseResponse {
    public int code;
    public String message;
    public String result;

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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
// 根据你的接口实际返回结构添加字段

}