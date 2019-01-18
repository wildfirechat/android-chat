// IConnectionStatusChangeListener.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements

interface IOnConnectionStatusChangeListener {
    void onConnectionStatusChange(int connectionStatus);
}
