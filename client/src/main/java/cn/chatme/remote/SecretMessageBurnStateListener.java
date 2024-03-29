/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

public interface SecretMessageBurnStateListener {
    void onSecretMessageStartBurning(String targetId, long playedMsgId);
    void onSecretMessageBurned(List<Long> messageIds);
}
