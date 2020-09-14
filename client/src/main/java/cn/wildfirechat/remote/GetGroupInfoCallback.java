/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.GroupInfo;

public interface GetGroupInfoCallback {

    void onSuccess(GroupInfo groupInfo);

    void onFail(int errorCode);
}
