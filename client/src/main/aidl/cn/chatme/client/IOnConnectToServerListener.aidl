// IConnectionStatusChangeListener.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements

interface IOnConnectToServerListener {
    void onConnectToServer(String host, String ip, int port);
}
