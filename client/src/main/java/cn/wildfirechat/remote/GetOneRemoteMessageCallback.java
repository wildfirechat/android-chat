/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.message.Message;

public interface GetOneRemoteMessageCallback {
    void onSuccess(Message messages);

    void onFail(int errorCode);
}
