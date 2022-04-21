/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface SecretMessageBurnStateListener {
    void onSecretMessageStartBurning(String targetId, long playedMsgId);
    void onSecretMessageBurned();
}
