/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import cn.wildfirechat.model.UserOnlineState;

public interface WatchOnlineStateCallback {
    void onSuccess(UserOnlineState[] userOnlineStates);

    void onFail(int errorCode);
}
