/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface CreateSecretChatCallback {
    void onSuccess(String targetId, int line);

    void onFail(int errorCode);
}
