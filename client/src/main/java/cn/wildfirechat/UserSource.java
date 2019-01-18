package cn.wildfirechat;

import java.util.List;

import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.SearchUserCallback;

public interface UserSource {
    UserInfo getUser(String userId);
    //List<UserInfo> getUsers(List<String> userIds);

    void searchUser(String keyword, final SearchUserCallback callback);

    void modifyMyInfo(List<ModifyMyInfoEntry> values, final GeneralCallback callback);
}
