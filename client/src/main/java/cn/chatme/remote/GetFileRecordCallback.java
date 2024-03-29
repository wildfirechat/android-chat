/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.FileRecord;

public interface GetFileRecordCallback {
    void onSuccess(List<FileRecord> records);

    void onFail(int errorCode);
}
