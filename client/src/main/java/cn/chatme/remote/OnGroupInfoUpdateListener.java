/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.GroupInfo;

public interface OnGroupInfoUpdateListener {
    void onGroupInfoUpdate(List<GroupInfo> groupInfos);
}
