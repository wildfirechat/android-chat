// IOnReceiveMessage.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.model.GroupInfo;

interface IOnGroupInfoUpdateListener {
    void onGroupInfoUpdated(in List<GroupInfo> groupInfos);
}
