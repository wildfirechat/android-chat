// IOnTrafficDataListener.aidl
package cn.chatme.client;



interface IOnSecretMessageBurnStateListener {
    void onSecretMessageStartBurning(in String targetId, in long playedMsgId);
    void onSecretMessageBurned(in int[] messageIds);
}
