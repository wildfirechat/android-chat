/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.Map;

import cn.chatme.model.UserOnlineState;

public interface OnUserOnlineEventListener {
    void onUserOnlineEvent(Map<String, UserOnlineState> userOnlineStateMap);
}
