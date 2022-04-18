/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.Map;

import cn.wildfirechat.model.UserOnlineState;

public interface SecretChatStateChangedListener {
    void onSecretChatStateChanged(String targetId, int state);
}
