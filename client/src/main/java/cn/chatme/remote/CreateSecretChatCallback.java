/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

public interface CreateSecretChatCallback {
    void onSuccess(String targetId, int line);

    void onFail(int errorCode);
}
