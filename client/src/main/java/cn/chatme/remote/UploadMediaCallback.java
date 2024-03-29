/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

public interface UploadMediaCallback {
    void onSuccess(String result);

    void onProgress(long uploaded, long total);

    void onFail(int errorCode);
}
