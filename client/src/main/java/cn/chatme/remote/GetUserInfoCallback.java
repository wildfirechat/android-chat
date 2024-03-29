/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.model.UserInfo;

public interface GetUserInfoCallback {

    void onSuccess(UserInfo userInfo);

    void onFail(int errorCode);
}
