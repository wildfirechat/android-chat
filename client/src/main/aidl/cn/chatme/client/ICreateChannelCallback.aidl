// IConnectionStatusChanged.aidl
package cn.chatme.client;

import cn.chatme.model.ChannelInfo;

interface ICreateChannelCallback {
    void onSuccess(in ChannelInfo channelInfo);
    void onFailure(in int errorCode);
}
