/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.ChatRoomInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

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
