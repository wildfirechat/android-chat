/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.GroupMember;

public interface OnGroupMembersUpdateListener {
    /**
     * 群成员信息更新通知
     *
     * @param groupId      群id
     * @param groupMembers 信息有更新的群成员，不是所有群成员!
     */
    void onGroupMembersUpdate(String groupId, List<GroupMember> groupMembers);
}
