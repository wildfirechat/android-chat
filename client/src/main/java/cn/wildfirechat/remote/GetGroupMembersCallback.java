package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;

public interface GetGroupMembersCallback {

    void onSuccess(List<GroupMember> groupMembers);

    void onFail(int errorCode);
}
