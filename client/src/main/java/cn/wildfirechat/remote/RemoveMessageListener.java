package cn.wildfirechat.remote;

import cn.wildfirechat.message.Message;

public interface RemoveMessageListener {
    void onMessagedRemoved(Message message);
}
