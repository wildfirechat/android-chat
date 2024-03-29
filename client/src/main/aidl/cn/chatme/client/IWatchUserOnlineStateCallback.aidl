// IOnReceiveMessage.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements
import cn.chatme.model.UserOnlineState;

interface IWatchUserOnlineStateCallback{
    void onSuccess(in UserOnlineState[] states);
    void onFailure(in int errorCode);
}
