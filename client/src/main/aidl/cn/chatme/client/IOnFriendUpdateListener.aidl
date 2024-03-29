// IOnReceiveMessage.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements

interface IOnFriendUpdateListener {
    void onFriendListUpdated(in List<String> friendList);
    void onFriendRequestUpdated(in List<String> newFriendRequestList);
}
