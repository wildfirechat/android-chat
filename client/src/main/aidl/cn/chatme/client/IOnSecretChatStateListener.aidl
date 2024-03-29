// IOnTrafficDataListener.aidl
package cn.chatme.client;



interface IOnSecretChatStateListener {
    void onSecretChatStateChanged(String targetid, int state);
}
