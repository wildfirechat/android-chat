package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.UserInfo;

public interface OnUserInfoUpdateListener {
    void onUserInfoUpdate(List<UserInfo> userInfos);
}
