/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.UserInfo;

public interface GetUserInfoListCallback {

    void onSuccess(List<UserInfo> userInfos);

    void onFail(int errorCode);
}
