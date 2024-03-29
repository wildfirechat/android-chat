// IOnReceiveMessage.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements
import cn.chatme.model.UserInfo;

interface IOnUserInfoUpdateListener {
    void onUserInfoUpdated(in List<UserInfo> userInfos);
}
