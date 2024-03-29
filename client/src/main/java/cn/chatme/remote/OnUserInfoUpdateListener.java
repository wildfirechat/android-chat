/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.UserInfo;

public interface OnUserInfoUpdateListener {
    void onUserInfoUpdate(List<UserInfo> userInfos);
}
