/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.model.ChatRoomMembersInfo;

public interface GetChatRoomMembersInfoCallback {
    void onSuccess(ChatRoomMembersInfo chatRoomMembersInfo);

    void onFail(int errorCode);
}
