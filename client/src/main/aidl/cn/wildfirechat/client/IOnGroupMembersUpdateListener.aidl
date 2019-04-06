// IOnReceiveMessage.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.model.GroupMember;

interface IOnGroupMembersUpdateListener {
    void onGroupMembersUpdated(in String groupId, in List<GroupMember> members);
}
