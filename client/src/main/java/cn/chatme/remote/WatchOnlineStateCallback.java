/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import cn.chatme.model.UserOnlineState;

public interface WatchOnlineStateCallback {
    void onSuccess(UserOnlineState[] userOnlineStates);

    void onFail(int errorCode);
}
