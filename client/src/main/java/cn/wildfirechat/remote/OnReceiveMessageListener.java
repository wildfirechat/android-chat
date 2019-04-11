package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.message.Message;


/**
 * * 当消息为{@link cn.wildfirechat.message.core.PersistFlag#No_Persist}也进行通知，当不需要是，需要自行处理
 */
public interface OnReceiveMessageListener {
    void onReceiveMessage(List<Message> messages, boolean hasMore);
}
