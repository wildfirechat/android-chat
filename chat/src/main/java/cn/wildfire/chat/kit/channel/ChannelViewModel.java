package cn.wildfire.chat.kit.channel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.third.utils.FileUtils;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;
import cn.wildfirechat.remote.OnChannelInfoUpdateListener;

public class ChannelViewModel extends ViewModel implements OnChannelInfoUpdateListener {

    private MutableLiveData<List<ChannelInfo>> channelInfoLiveData;

    public MutableLiveData<List<ChannelInfo>> channelInfoLiveData() {
        if (channelInfoLiveData == null) {
            channelInfoLiveData = new MutableLiveData<>();
        }
        return channelInfoLiveData;
    }

    public ChannelViewModel() {
        super();
        ChatManager.Instance().addChannelInfoUpdateListener(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeChannelInfoListener(this);
    }

    public MutableLiveData<OperateResult<String>> createChannel(String channelId, String channelName, String channelPortrait, String desc, String extra) {
        MutableLiveData<OperateResult<String>> resultLiveData = new MutableLiveData<>();
        byte[] content = FileUtils.readFile(channelPortrait);
        if (content != null) {
            ChatManager.Instance().uploadMedia(content, MessageContentMediaType.PORTRAIT.getValue(), new GeneralCallback2() {
                @Override
                public void onSuccess(String result) {
                    ChatManager.Instance().createChannel(channelId, channelName, result, desc, extra, new GeneralCallback2() {
                        @Override
                        public void onSuccess(String result) {
                            resultLiveData.setValue(new OperateResult<String>(result, 0));
                        }

                        @Override
                        public void onFail(int errorCode) {
                            resultLiveData.setValue(new OperateResult<String>(errorCode));
                        }
                    });
                }

                @Override
                public void onFail(int errorCode) {
                    resultLiveData.setValue(new OperateResult<>(errorCode));

                }
            });
        } else {
            resultLiveData.setValue(new OperateResult<>("生成头像失败", -1));
        }

        return resultLiveData;
    }

    public ChannelInfo getChannelInfo(String channelId, boolean refresh) {
        return ChatManager.Instance().getChannelInfo(channelId, refresh);
    }

    public boolean isListenedChannel(String channelId) {
        return ChatManager.Instance().isListenedChannel(channelId);
    }

    public MutableLiveData<OperateResult<Boolean>> listenChannel(String channelId, boolean listen) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().listenChannel(channelId, listen, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public List<ChannelInfo> getMyChannels() {
        List<String> channelIds = ChatManager.Instance().getMyChannels();
        if (channelIds != null && !channelIds.isEmpty()) {
            List<ChannelInfo> channelInfos = new ArrayList<>(channelIds.size());
            for (String channelId : channelIds) {
                ChannelInfo channelInfo = ChatManager.Instance().getChannelInfo(channelId, false);
                if (channelInfo == null) {
                    channelInfo = new ChannelInfo();
                    channelInfo.channelId = channelId;
                }
                channelInfos.add(channelInfo);
            }
            return channelInfos;
        }
        return null;
    }

    public List<ChannelInfo> getListenedChannels() {
        List<String> channelIds = ChatManager.Instance().getListenedChannels();
        if (channelIds != null && !channelIds.isEmpty()) {
            List<ChannelInfo> channelInfos = new ArrayList<>(channelIds.size());
            for (String channelId : channelIds) {
                ChannelInfo channelInfo = ChatManager.Instance().getChannelInfo(channelId, false);
                if (channelInfo == null) {
                    channelInfo = new ChannelInfo();
                    channelInfo.channelId = channelId;
                }
                channelInfos.add(channelInfo);
            }
            return channelInfos;
        }
        return null;
    }

    @Override
    public void onChannelInfoUpdate(List<ChannelInfo> channelInfos) {
        if (channelInfoLiveData != null) {
            channelInfoLiveData.setValue(channelInfos);
        }
    }
}
