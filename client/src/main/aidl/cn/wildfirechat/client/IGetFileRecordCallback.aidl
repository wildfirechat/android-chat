// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.FileRecord;

interface IGetFileRecordCallback {
    void onSuccess(in List<FileRecord> messages);
    void onFailure(in int errorCode);
}
