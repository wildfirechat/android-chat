package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.UserInfo;

public interface OnUserInfoUpdateListener {
    void onUserInfoUpdated(List<UserInfo> userInfos);
}
