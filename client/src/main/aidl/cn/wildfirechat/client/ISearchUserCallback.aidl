// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.UserInfo;

interface ISearchUserCallback {
    void onSuccess(in List<UserInfo> userInfos);
    void onFailure(in int errorCode);
}
