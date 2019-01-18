package cn.wildfirechat.client;

import cn.wildfirechat.model.ChannelInfo;

interface IOnChannelInfoUpdateListener {
    void onChannelInfoUpdated(in List<ChannelInfo> channelInfos);
}
