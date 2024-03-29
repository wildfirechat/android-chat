// IOnReceiveMessage.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements
import cn.chatme.model.UserOnlineState;

interface IOnUserOnlineEventListener {
    void onUserOnlineEvent(in UserOnlineState[] states);
}
