// IOnReceiveMessage.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.model.UserInfo;

interface IOnUserInfoUpdateListener {
    void onUserInfoUpdated(in List<UserInfo> userInfos);
}
