// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.model.GroupInfo;

interface IGetGroupsCallback {
    void onSuccess(in List<GroupInfo> groupInfos);
    void onFailure(in int errorCode);
}
