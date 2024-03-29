/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

public interface SecretChatStateChangeListener {
    void onSecretChatStateChanged(String targetId, ChatManager.SecretChatState state);
}
