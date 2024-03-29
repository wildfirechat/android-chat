/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.ChannelInfo;

public interface OnChannelInfoUpdateListener {
    void onChannelInfoUpdate(List<ChannelInfo> channelInfos);
}
