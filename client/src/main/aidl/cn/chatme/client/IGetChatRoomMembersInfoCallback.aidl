// IMGetChatroomInfoCallback.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements
import cn.chatme.model.ChatRoomMembersInfo;

interface IGetChatRoomMembersInfoCallback {
    void onSuccess(in ChatRoomMembersInfo chatRoomMembersInfo);
    void onFailure(in int errorCode);
}
