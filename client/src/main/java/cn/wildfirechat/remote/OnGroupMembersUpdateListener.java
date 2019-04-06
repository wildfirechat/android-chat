package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.GroupMember;

public interface OnGroupMembersUpdateListener {
    /**
     * 群成员信息更新通知
     *
     * @param groupId      群id
     * @param groupMembers 信息有更新的群成员，不是所有群成员!
     */
    void onGroupMembersUpdate(String groupId, List<GroupMember> groupMembers);
}
