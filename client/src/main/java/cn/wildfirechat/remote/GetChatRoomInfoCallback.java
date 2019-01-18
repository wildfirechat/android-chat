package cn.wildfirechat.remote;

import cn.wildfirechat.model.ChatRoomInfo;

public interface GetChatRoomInfoCallback {
    void onSuccess(ChatRoomInfo chatRoomInfo);

    void onFailure(int errorCode);
}
