// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.model.GroupInfo;

interface IGetGroupInfoCallback {
    void onSuccess(in GroupInfo groupInfo);
    void onFailure(in int errorCode);
}
