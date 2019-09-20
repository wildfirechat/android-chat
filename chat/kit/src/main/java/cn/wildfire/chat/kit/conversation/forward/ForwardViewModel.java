package cn.wildfire.chat.kit.conversation.forward;

import java.util.concurrent.atomic.AtomicInteger;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.SendMessageCallback;

public class ForwardViewModel extends ViewModel {
    public MutableLiveData<OperateResult<Integer>> forward(Conversation targetConversation, Message... messages) {
        MutableLiveData<OperateResult<Integer>> resultMutableLiveData = new MutableLiveData<>();
        AtomicInteger count = new AtomicInteger(0);

        for (Message message : messages) {
            if (message != null) {
                count.addAndGet(1);
            }
        }

        for (Message message : messages) {
            if (message == null) {
                continue;
            }
            message.conversation = targetConversation;
            ChatManager.Instance().sendMessage(message, new SendMessageCallback() {
                @Override
                public void onSuccess(long messageUid, long timestamp) {
                    if (count.decrementAndGet() == 0) {
                        resultMutableLiveData.postValue(new OperateResult<>(0));
                    }
                }

                @Override
                public void onFail(int errorCode) {
                    if (count.decrementAndGet() == 0) {
                        resultMutableLiveData.postValue(new OperateResult<>(errorCode));
                    }
                }

                @Override
                public void onPrepare(long messageId, long savedTime) {

                }
            });
        }
        return resultMutableLiveData;
    }
}
