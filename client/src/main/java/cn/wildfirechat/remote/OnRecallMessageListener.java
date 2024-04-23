/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import cn.wildfirechat.message.Message;

public interface OnRecallMessageListener {
    /**
     * @param message 被撤回的消息，以下几种情况下，message 里面，只有 messageUid 字段有效
     *               <pre>
     *
     *               1.配置{@link cn.wildfirechat.message.notification.RecallMessageContent} 的
     *               存储类型为{@link cn.wildfirechat.message.core.PersistFlag#No_Persist}
     *
     *               2. 撤回消息时，该消息已被删除
     *
     *               </pre>
     */
    void onRecallMessage(Message message);
}
