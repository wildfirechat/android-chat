// IMGetChatroomInfoCallback.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.model.ChatRoomMembersInfo;

interface IGetChatRoomMembersInfoCallback {
    void onSuccess(in ChatRoomMembersInfo chatRoomMembersInfo);
    void onFailure(in int errorCode);
}
