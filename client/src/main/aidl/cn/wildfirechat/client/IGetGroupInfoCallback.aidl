// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.GroupInfo;

interface IGetGroupInfoCallback {
    void onSuccess(in GroupInfo groupInfo);
    void onFailure(in int errorCode);
}
