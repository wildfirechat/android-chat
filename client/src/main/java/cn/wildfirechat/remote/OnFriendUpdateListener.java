package cn.wildfirechat.remote;

import java.util.List;

public interface OnFriendUpdateListener {
    void onFriendListUpdate(List<String> updateFriendList);

    void onFriendRequestUpdate();
}
