/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

public interface OnFriendUpdateListener {
    /**
     * 好友列表更新回调
     * @param updateFriendList 只包含有更新的好友，不包含被删除的好友，所以这个回调的时候，需要调用一下{@link ChatManager#getMyFriendList(boolean)}获取当前完整的好友列表
     */
    void onFriendListUpdate(List<String> updateFriendList);

    /**
     * 好友请求列表更新回调
     * @param newRequests 只包含新来的好友请求，所以这个回调的时候，需要调用一下{@link ChatManager#getFriendRequest(boolean)}获取当前完整的好友请求列表
     */
    void onFriendRequestUpdate(List<String> newRequests);
}
