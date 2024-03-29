/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

/**
 * im 进程状态监听
 */
public interface IMServiceStatusListener {
    void onServiceConnected();

    void onServiceDisconnected();
}
