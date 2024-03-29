/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.GroupMember;

public interface GetGroupMembersCallback {

    void onSuccess(List<GroupMember> groupMembers);

    void onFail(int errorCode);
}
