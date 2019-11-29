package cn.wildfire.chat.app.login.model;

public class RegResult {

    //{"code":0,"msg":"success","result":{"userId":"cic6c6EE","name":"13774513094"}}

    public String code="";
    public String getName() {
        return code;
    }

    public void setName(String code) {
        this.code = code;
    }

    public String msg="";
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public RegJsonResult result = new RegJsonResult();


}
