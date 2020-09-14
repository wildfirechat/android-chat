/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import cn.wildfirechat.model.ChatRoomInfo;

public interface GetChatRoomInfoCallback {
    void onSuccess(ChatRoomInfo chatRoomInfo);

    void onFail(int errorCode);
}
