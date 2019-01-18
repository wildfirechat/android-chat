// IMGetChatroomInfoCallback.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.model.ChatRoomInfo;

interface IGetChatRoomInfoCallback {
    void onSuccess(in ChatRoomInfo chatRoomInfo);
    void onFailure(in int errorCode);
}
