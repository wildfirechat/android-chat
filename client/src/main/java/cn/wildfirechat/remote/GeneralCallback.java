/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface GeneralCallback {
    void onSuccess();

    void onFail(int errorCode);
}
