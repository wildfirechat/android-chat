// IConnectionStatusChangeListener.aidl
package cn.chatme.client;

// Declare any non-default types here with import statements

interface IOnConnectionStatusChangeListener {
    void onConnectionStatusChange(int connectionStatus);
}
