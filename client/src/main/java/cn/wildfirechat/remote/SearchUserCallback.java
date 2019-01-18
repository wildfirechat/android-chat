package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.UserInfo;

public interface SearchUserCallback {
    void onSuccess(List<UserInfo> userInfos);

    void onFailure(int errorCode);
}
