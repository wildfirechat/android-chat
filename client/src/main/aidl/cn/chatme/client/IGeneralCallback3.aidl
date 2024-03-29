// IConnectionStatusChanged.aidl
package cn.chatme.client;

import java.util.List;


interface IGeneralCallback3 {
    void onSuccess(in List<String> results);
    void onFailure(in int errorCode);
}
