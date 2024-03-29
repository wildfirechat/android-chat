/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.message.Message;

public interface GetOneRemoteMessageCallback {
    void onSuccess(Message message);

    void onFail(int errorCode);
}
