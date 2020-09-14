/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

public interface UploadMediaCallback {
    void onSuccess(String result);

    void onProgress(long uploaded, long total);

    void onFail(int errorCode);
}
