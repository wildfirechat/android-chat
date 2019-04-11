package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.ChannelInfo;

public interface OnChannelInfoUpdateListener {
    void onChannelInfoUpdate(List<ChannelInfo> channelInfos);
}
