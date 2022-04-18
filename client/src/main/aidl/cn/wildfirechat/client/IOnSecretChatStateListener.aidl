// IOnTrafficDataListener.aidl
package cn.wildfirechat.client;



interface IOnSecretChatStateListener {
    void onSecretChatStateChanged(String targetid, int state);
}
