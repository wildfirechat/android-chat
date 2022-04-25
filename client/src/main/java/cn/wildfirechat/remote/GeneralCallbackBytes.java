/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface GeneralCallbackBytes {
    void onSuccess(byte[] data);

    void onFail(int errorCode);
}
