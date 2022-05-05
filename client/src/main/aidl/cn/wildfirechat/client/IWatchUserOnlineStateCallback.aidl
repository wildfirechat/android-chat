// IOnReceiveMessage.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.model.UserOnlineState;

interface IWatchUserOnlineStateCallback{
    void onSuccess(in UserOnlineState[] states);
    void onFailure(in int errorCode);
}
