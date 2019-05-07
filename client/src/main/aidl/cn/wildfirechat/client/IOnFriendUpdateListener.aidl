// IOnReceiveMessage.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements

interface IOnFriendUpdateListener {
    void onFriendListUpdated(in List<String> friendList);
    void onFriendRequestUpdated();
}
