// IMGetChatroomInfoCallback.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements
import cn.chatme.model.ChatRoomInfo;

interface IGetChatRoomInfoCallback {
    void onSuccess(in ChatRoomInfo chatRoomInfo);
    void onFailure(in int errorCode);
}
