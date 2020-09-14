/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

/**
 * im 进程状态监听
 */
public interface IMServiceStatusListener {
    void onServiceConnected();

    void onServiceDisconnected();
}
