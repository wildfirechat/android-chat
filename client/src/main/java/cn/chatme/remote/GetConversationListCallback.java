/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.ConversationInfo;

public interface GetConversationListCallback {

    void onSuccess(List<ConversationInfo> conversationInfos);

    void onFail(int errorCode);
}
