/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.ReadEntry;

/**
 * 消息已读
 */
public interface OnMessageReadListener {
    void onMessageRead(List<ReadEntry> readEntries);
}
