/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.model.GroupInfo;

public interface GetGroupInfoCallback {

    void onSuccess(GroupInfo groupInfo);

    void onFail(int errorCode);
}
