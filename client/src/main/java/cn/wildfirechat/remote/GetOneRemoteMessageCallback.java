/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import cn.wildfirechat.message.Message;

public interface GetOneRemoteMessageCallback {
    void onSuccess(Message message);

    void onFail(int errorCode);
}
