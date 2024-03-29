// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.model.ChannelInfo;

interface ISearchChannelCallback {
    void onSuccess(in List<ChannelInfo> channels);
    void onFailure(in int errorCode);
}
