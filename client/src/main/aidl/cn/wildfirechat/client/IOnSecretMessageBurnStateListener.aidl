// IOnTrafficDataListener.aidl
package cn.wildfirechat.client;



interface IOnSecretMessageBurnStateListener {
    void onSecretMessageStartBurning(String targetId, long playedMsgId);
    void onSecretMessageBurned();
}
