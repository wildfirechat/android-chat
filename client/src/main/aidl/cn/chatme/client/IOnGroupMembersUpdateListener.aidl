// IOnReceiveMessage.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements
import cn.chatme.model.GroupMember;

interface IOnGroupMembersUpdateListener {
    void onGroupMembersUpdated(in String groupId, in List<GroupMember> members);
}
