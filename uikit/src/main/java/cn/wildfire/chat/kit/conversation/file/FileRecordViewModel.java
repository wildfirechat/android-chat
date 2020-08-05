package cn.wildfire.chat.kit.conversation.file;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.FileRecord;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GetFileRecordCallback;

public class FileRecordViewModel extends ViewModel {
    public LiveData<OperateResult<List<FileRecord>>> getConversationFileRecords(Conversation conversation, long beforeMessageUid, int count) {
        MutableLiveData<OperateResult<List<FileRecord>>> data = new MutableLiveData<>();
        ChatManager.Instance().getConversationFileRecords(conversation, beforeMessageUid, count, new GetFileRecordCallback() {
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
        ChatManager.Instance().getMyFileRecords(beforeMessageUid, count, new GetFileRecordCallback() {
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
