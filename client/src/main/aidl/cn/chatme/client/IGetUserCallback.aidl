// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.model.UserInfo;

interface IGetUserCallback {
    void onSuccess(in UserInfo userInfo);
    void onFailure(in int errorCode);
}
