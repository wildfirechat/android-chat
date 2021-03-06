/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface GetUploadUrlCallback {
    void onSuccess(String uploadUrl, String remoteUrl, String backUploadupUrl, int serverType);
    void onFail(int errorCode);
}
