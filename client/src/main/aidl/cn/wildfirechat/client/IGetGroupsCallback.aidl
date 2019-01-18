// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.GroupInfo;

interface IGetGroupsCallback {
    void onSuccess(in List<GroupInfo> groupInfos);
    void onFailure(in int errorCode);
}
