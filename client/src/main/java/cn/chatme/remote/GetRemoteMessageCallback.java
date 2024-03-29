/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.message.Message;

public interface GetRemoteMessageCallback {
    void onSuccess(List<Message> messages);

    void onFail(int errorCode);
}
