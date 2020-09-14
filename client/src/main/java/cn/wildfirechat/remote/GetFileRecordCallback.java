/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.FileRecord;

public interface GetFileRecordCallback {
    void onSuccess(List<FileRecord> records);

    void onFail(int errorCode);
}
