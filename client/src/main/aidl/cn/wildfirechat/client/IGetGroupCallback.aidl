// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.GroupInfo;

interface IGetGroupCallback {
    void onSuccess(in GroupInfo groupInfo);
    void onFailure(in int errorCode);
}
