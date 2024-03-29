/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.remote;

import java.util.List;

import cn.chatme.model.ChannelInfo;
import cn.chatme.model.ChatRoomInfo;
import cn.chatme.model.GroupInfo;
import cn.chatme.model.UserInfo;

public interface DefaultPortraitProvider {
    String userDefaultPortrait(UserInfo info);

    String groupDefaultPortrait(GroupInfo groupInfo, List<UserInfo> first9MemberInfos);

    default String channelDefaultPortrait(ChannelInfo info) {
        return info.portrait;
    }

    default String chatRoomDefaultPortrait(ChatRoomInfo info) {
        return info.portrait;
    }
}
