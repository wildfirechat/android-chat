/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface OnConnectToServerListener {
    void onConnectToServer(String host, String ip, int port);
}
