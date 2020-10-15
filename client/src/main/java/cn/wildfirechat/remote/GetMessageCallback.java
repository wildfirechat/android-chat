/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.message.Message;

public interface GetMessageCallback {
    /**
     * 获取消息回调
     *
     * @param messages 本次回调的消息列表
     * @param hasMore  由于ipc调用数据大小有限制，如果消息数据过大就无法一次返回，需要多次返回，hasMore用来说明是不是后续还有数据回调上来。
     */
    void onSuccess(List<Message> messages, boolean hasMore);

    void onFail(int errorCode);
}
