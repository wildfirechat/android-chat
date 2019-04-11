package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.ChannelInfo;

public interface SearchChannelCallback {
    void onSuccess(List<ChannelInfo> channelInfos);

    void onFail(int errorCode);
}
