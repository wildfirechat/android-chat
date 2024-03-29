/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

public interface GeneralCallback {
    void onSuccess();

    void onFail(int errorCode);
}
