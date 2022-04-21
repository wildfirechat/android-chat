/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface SecretChatStateChangeListener {
    void onSecretChatStateChanged(String targetId, ChatManager.SecretChatState state);
}
