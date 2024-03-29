package cn.chatme.client;

import cn.chatme.model.ChannelInfo;

interface IOnChannelInfoUpdateListener {
    void onChannelInfoUpdated(in List<ChannelInfo> channelInfos);
}
