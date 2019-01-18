package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.ChannelInfo;

public interface OnChannelInfoUpdateListener {
    void onChannelInfoUpdated(List<ChannelInfo> channelInfos);
}
