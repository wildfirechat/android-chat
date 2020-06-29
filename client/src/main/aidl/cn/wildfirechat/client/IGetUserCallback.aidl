// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.UserInfo;

interface IGetUserCallback {
    void onSuccess(in UserInfo userInfo);
    void onFailure(in int errorCode);
}
