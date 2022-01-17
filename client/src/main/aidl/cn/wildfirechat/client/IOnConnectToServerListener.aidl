// IConnectionStatusChangeListener.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements

interface IOnConnectToServerListener {
    void onConnectToServer(String host, String ip, int port);
}
