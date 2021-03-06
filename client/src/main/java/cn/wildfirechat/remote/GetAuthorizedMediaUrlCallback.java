/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface GetAuthorizedMediaUrlCallback {
    void onSuccess(String url, String backupUrl);
    void onFail(int errorCode);
}
