/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.Map;

import cn.wildfirechat.model.UserOnlineState;

public interface OnUserOnlineEventListener {
    void onUserOnlineEvent(Map<String, UserOnlineState> userOnlineStateMap);
}
