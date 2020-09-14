/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.UserInfo;

public interface OnUserInfoUpdateListener {
    void onUserInfoUpdate(List<UserInfo> userInfos);
}
