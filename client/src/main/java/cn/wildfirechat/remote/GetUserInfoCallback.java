package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public interface GetUserInfoCallback {

    void onSuccess(UserInfo userInfo);

    void onFail(int errorCode);
}
