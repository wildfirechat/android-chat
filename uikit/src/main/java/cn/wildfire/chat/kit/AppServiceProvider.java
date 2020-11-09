/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.app.Application;

import java.util.List;

import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.group.GroupAnnouncement;
import cn.wildfire.chat.kit.net.SimpleCallback;


public interface AppServiceProvider {
    public interface UpdateGroupAnnouncementCallback {
        void onUiSuccess(GroupAnnouncement announcement);

        void onUiFailure(int code, String msg);
    }

    public interface GetGroupAnnouncementCallback {
        void onUiSuccess(GroupAnnouncement announcement);

        void onUiFailure(int code, String msg);
    }

    interface GetFavoriteItemCallback {
        void onUiSuccess(List<FavoriteItem> items, boolean hasMore);

        void onUiFailure(int code, String msg);
    }

    public void getGroupAnnouncement(String groupId, GetGroupAnnouncementCallback callback);

    public void updateGroupAnnouncement(String groupId, String announcement, UpdateGroupAnnouncementCallback callback);

    void showPCLoginActivity(String userId, String token, int platform);

    /**
     * 前置条件是已经调过{@link cn.wildfirechat.remote.ChatManager#init(Application, String)}
     *
     * @param callback
     */
    void uploadLog(SimpleCallback<String> callback);

    void changeName(String newName, SimpleCallback<Void> callback);

    void getFavoriteItems(int startId, int count, GetFavoriteItemCallback callback);

    void addFavoriteItem(FavoriteItem item, SimpleCallback<Void> callback);

    void removeFavoriteItem(int favId, SimpleCallback<Void> callback);

}
