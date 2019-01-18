package cn.wildfirechat.remote;

import cn.wildfirechat.model.ChatRoomMembersInfo;

public interface GetChatRoomMembersInfoCallback {
    void onSuccess(ChatRoomMembersInfo chatRoomMembersInfo);

    void onFailure(int errorCode);
}
