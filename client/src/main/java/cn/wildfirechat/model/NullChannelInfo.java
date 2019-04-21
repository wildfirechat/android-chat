package cn.wildfirechat.model;

public class NullChannelInfo extends ChannelInfo {
    public NullChannelInfo(String channelId) {
        this.channelId = channelId;
        this.name = "<" + channelId + ">";
        this.owner = "";
    }
}
