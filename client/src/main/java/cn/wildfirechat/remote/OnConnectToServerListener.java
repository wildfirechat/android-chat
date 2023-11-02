/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface OnConnectToServerListener {
    /**
     * @param host route host 或 connect host
     * @param ip   "" 表示 route 操作；非空表示 connect 操作，值为 host dns 解析的结果
     * @param port route port 或 connect port
     */
    void onConnectToServer(String host, String ip, int port);
}
