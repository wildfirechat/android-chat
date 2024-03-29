/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.GroupInfo;

public interface GetGroupsCallback {

    void onSuccess(List<GroupInfo> groupInfos);

    void onFail(int errorCode);
}
