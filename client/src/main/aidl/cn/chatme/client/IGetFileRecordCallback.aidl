// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.model.FileRecord;

interface IGetFileRecordCallback {
    void onSuccess(in List<FileRecord> messages);
    void onFailure(in int errorCode);
}
