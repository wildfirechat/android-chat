package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.message.Message;

public interface GetRemoteMessageCallback {
    void onSuccess(List<Message> messages);
    void onFail(int errorCode);
}
