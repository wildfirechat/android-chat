// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.model.UserInfo;

interface ISearchUserCallback {
    void onSuccess(in List<UserInfo> userInfos);
    void onFailure(in int errorCode);
}
