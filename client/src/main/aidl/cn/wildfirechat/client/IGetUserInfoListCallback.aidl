// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.UserInfo;

interface IGetUserInfoListCallback {
    void onSuccess(in List<UserInfo> infos, in boolean hasMore);
    void onFailure(in int errorCode);
}
