// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.ChannelInfo;

interface ICreateChannelCallback {
    void onSuccess(in ChannelInfo channelInfo);
    void onFailure(in int errorCode);
}
