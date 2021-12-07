/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.client;

/**
 * Created by heavyrainlee on 2018/1/26.
 */

public interface ConnectionStatus {
    //错误码kConnectionStatusKickedoff是IM服务2021.9.15之后的版本才支持，并且打开服务器端开关server.client_support_kickoff_event
    int ConnectionStatusKickedoff = -7;
    int ConnectionStatusSecretKeyMismatch = -6;
    int ConnectionStatusTokenIncorrect = -5;
    int ConnectionStatusServerDown = -4;
    int ConnectionStatusRejected = -3;
    int ConnectionStatusLogout = -2;
    int ConnectionStatusUnconnected = -1;
    int ConnectionStatusConnecting = 0;
    int ConnectionStatusConnected = 1;
    int ConnectionStatusReceiveing = 2;
}
