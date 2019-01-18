// IOnReceiveMessage.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.message.Message;

interface IOnReceiveMessageListener {
    void onReceive(in List<Message> messages, boolean hasMore);
    void onRecall(in long messageUid);
}
