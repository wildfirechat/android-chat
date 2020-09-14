/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public interface GetUserInfoCallback {

    void onSuccess(UserInfo userInfo);

    void onFail(int errorCode);
}
