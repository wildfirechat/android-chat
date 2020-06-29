// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.GroupMember;

interface IGetGroupMemberCallback {
    void onSuccess(in List<GroupMember> members);
    void onFailure(in int errorCode);
}
