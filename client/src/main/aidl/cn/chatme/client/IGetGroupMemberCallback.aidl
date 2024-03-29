// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.model.GroupMember;

interface IGetGroupMemberCallback {
    void onSuccess(in List<GroupMember> members, boolean hasMore);
    void onFailure(in int errorCode);
}
