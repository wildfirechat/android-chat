// IOnReceiveMessage.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements
import cn.chatme.message.Message;
import cn.chatme.model.ReadEntry;

interface IOnReceiveMessageListener {
    void onReceive(in List<Message> messages, boolean hasMore);
    void onRecall(in long messageUid);
    void onDelete(in long messageUid);
    void onDelivered(in Map deliveryMap);
    void onReaded(in List<ReadEntry> readEntrys);
}
