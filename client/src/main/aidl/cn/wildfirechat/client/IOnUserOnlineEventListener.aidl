// IOnReceiveMessage.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.model.UserOnlineState;

interface IOnUserOnlineEventListener {
    void onUserOnlineEvent(in UserOnlineState[] states);
}
