/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.ChannelInfo;

public interface SearchChannelCallback {
    void onSuccess(List<ChannelInfo> channelInfos);

    void onFail(int errorCode);
}
