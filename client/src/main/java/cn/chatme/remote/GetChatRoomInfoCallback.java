/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.model.ChatRoomInfo;

public interface GetChatRoomInfoCallback {
    void onSuccess(ChatRoomInfo chatRoomInfo);

    void onFail(int errorCode);
}
