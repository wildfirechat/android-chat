/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import cn.wildfirechat.message.Message;

public interface OnDeleteMessageListener {
    void onDeleteMessage(Message message);
}
