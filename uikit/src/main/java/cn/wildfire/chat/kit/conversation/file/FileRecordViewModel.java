/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.file;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import cn.wildfire.chat.kit.common.OperateResult;
import cn.chatme.model.Conversation;
import cn.chatme.model.FileRecord;
import cn.chatme.model.FileRecordOrder;
import cn.chatme.remote.ChatManager;
import cn.chatme.remote.GeneralCallback;
import cn.chatme.remote.GetFileRecordCallback;

public class FileRecordViewModel extends ViewModel {
    public LiveData<OperateResult<List<FileRecord>>> getConversationFileRecords(Conversation conversation, String fromUser, long beforeMessageUid, int count) {
        MutableLiveData<OperateResult<List<FileRecord>>> data = new MutableLiveData<>();
        ChatManager.Instance().getConversationFileRecords(conversation, fromUser, beforeMessageUid, FileRecordOrder.By_Size_Desc, count, new GetFileRecordCallback() {
            @Override
            public void onSuccess(List<FileRecord> records) {
                data.postValue(new OperateResult<>(records, 0));
            }

            @Override
            public void onFail(int errorCode) {
                data.postValue(new OperateResult<>(-1));
            }
        });
        return data;
    }

    public LiveData<OperateResult<List<FileRecord>>> getMyFileRecords(long beforeMessageUid, int count) {
        MutableLiveData<OperateResult<List<FileRecord>>> data = new MutableLiveData<>();
        ChatManager.Instance().getMyFileRecords(beforeMessageUid, FileRecordOrder.By_Size_Desc, count, new GetFileRecordCallback() {
            @Override
            public void onSuccess(List<FileRecord> records) {
                data.postValue(new OperateResult<>(records, 0));
            }

            @Override
            public void onFail(int errorCode) {
                data.postValue(new OperateResult<>(-1));
            }
        });
        return data;
    }

    public LiveData<OperateResult<Boolean>> deleteFileRecord(long messageUid) {
        MutableLiveData<OperateResult<Boolean>> data = new MutableLiveData<>();
        ChatManager.Instance().deleteFileRecord(messageUid, new GeneralCallback() {
            @Override
            public void onSuccess() {
                data.postValue(new OperateResult<>(true, 0));
            }

            @Override
            public void onFail(int errorCode) {
                data.postValue(new OperateResult<>(-1));
            }
        });
        return data;
    }
}
