/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import cn.wildfire.chat.kit.Config;

/**
 * 当前直播会话的状态快照（不可变值对象）。
 * 由 {@link LiveStreamingKit} 在直播 WebRTC 会话连接成功后创建。
 */
public class LiveSession {
    public final String callId;
    public final boolean audioOnly;
    public final String pin;
    public final String hostUserId;
    public final String title;

    public LiveSession(String callId, boolean audioOnly, String pin, String hostUserId, String title) {
        this.callId = callId;
        this.audioOnly = audioOnly;
        this.pin = pin;
        this.hostUserId = hostUserId;
        this.title = title;
    }

    /** HLS 直播观看地址 */
    public String getHlsUrl() {
        return Config.LIVE_STREAMING_ADDRESS + callId + "/stream.m3u8";
    }
}
