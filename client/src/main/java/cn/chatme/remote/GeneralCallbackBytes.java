/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

public interface GeneralCallbackBytes {
    void onSuccess(byte[] data);

    void onFail(int errorCode);
}
