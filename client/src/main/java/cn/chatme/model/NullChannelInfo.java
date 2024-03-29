/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.model;

public class NullChannelInfo extends ChannelInfo {
    public NullChannelInfo(String channelId) {
        this.channelId = channelId;
        //this.name = "<" + channelId + ">";
        this.name = "频道";
        this.owner = "";
    }
}
