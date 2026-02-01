/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

/**
 * 空频道信息类
 * <p>
 * 当频道信息不存在时返回的空对象实现。
 * 使用Null Object模式，避免上层代码不断的做空值检查。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class NullChannelInfo extends ChannelInfo {
    public NullChannelInfo(String channelId) {
        this.channelId = channelId;
        //this.name = "<" + channelId + ">";
        this.name = "频道";
        this.owner = "";
    }
}
