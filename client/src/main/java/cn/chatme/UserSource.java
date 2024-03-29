/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme;

import java.util.List;

import cn.chatme.model.ModifyMyInfoEntry;
import cn.chatme.model.UserInfo;
import cn.chatme.remote.GeneralCallback;
import cn.chatme.remote.SearchUserCallback;

public interface UserSource {
    UserInfo getUser(String userId);
    //List<UserInfo> getUsers(List<String> userIds);

    void searchUser(String keyword, final SearchUserCallback callback);

    void modifyMyInfo(List<ModifyMyInfoEntry> values, final GeneralCallback callback);
}
