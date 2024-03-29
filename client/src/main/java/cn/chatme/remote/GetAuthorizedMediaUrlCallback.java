/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

public interface GetAuthorizedMediaUrlCallback {
    void onSuccess(String url, String backupUrl);
    void onFail(int errorCode);
}
