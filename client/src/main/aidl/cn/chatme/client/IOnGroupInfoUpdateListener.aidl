// IOnReceiveMessage.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements
import cn.chatme.model.GroupInfo;

interface IOnGroupInfoUpdateListener {
    void onGroupInfoUpdated(in List<GroupInfo> groupInfos);
}
