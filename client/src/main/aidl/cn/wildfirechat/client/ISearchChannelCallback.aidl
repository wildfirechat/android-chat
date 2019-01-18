// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.ChannelInfo;

interface ISearchChannelCallback {
    void onSuccess(in List<ChannelInfo> channels);
    void onFailure(in int errorCode);
}
