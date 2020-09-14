/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.ReadEntry;

/**
 * 消息已读
 */
public interface OnMessageReadListener {
    void onMessageRead(List<ReadEntry> readEntries);
}
