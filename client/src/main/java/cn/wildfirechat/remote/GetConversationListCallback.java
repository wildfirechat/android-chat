/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.ConversationInfo;

public interface GetConversationListCallback {

    void onSuccess(List<ConversationInfo> conversationInfos);

    void onFail(int errorCode);
}
