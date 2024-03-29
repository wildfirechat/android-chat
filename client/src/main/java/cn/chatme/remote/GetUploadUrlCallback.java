/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

public interface GetUploadUrlCallback {
    void onSuccess(String uploadUrl, String remoteUrl, String backUploadUrl, int serverType);
    void onFail(int errorCode);
}
