/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.app.Application;

import java.util.List;

import cn.wildfirechat.model.UserIdNamePortrait;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.group.GroupAnnouncement;
import cn.wildfire.chat.kit.net.BooleanCallback;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;


public interface AppServiceProvider {
    interface UpdateGroupAnnouncementCallback {
        void onUiSuccess(GroupAnnouncement announcement);

        void onUiFailure(int code, String msg);
    }

    interface GetGroupAnnouncementCallback {
        void onUiSuccess(GroupAnnouncement announcement);

        void onUiFailure(int code, String msg);
    }

    interface GetGroupMemberForPotraitCallback {
        void onUiSuccess(List<UserIdNamePortrait> userIdNamePortraits);

        void onUiFailure(int code, String msg);
    }

    interface GetFavoriteItemCallback {
        void onUiSuccess(List<FavoriteItem> items, boolean hasMore);

        void onUiFailure(int code, String msg);
    }

    void getGroupAnnouncement(String groupId, GetGroupAnnouncementCallback callback);

    void updateGroupAnnouncement(String groupId, String announcement, UpdateGroupAnnouncementCallback callback);

    default void getGroupPortrait(String groupId, SimpleCallback<String> callback) {

    }

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

    void getMyPrivateConferenceId(GeneralCallback2 callback);

    void createConference(ConferenceInfo info, GeneralCallback2 callback);

    void queryConferenceInfo(String conferenceId, String password, QueryConferenceInfoCallback callback);

    void destroyConference(String conferenceId, GeneralCallback callback);

    void favConference(String conferenceId, GeneralCallback callback);

    void unfavConference(String conferenceId, GeneralCallback callback);

    void isFavConference(String conferenceId, BooleanCallback callback);

    void getFavConferences(FavConferenceCallback callback);

    void updateConference(ConferenceInfo conferenceInfo, GeneralCallback callback);

    void recordConference(String conferenceId, boolean record, GeneralCallback callback);

    void setConferenceFocusUserId(String conferenceId, String userId, GeneralCallback callback);

    interface QueryConferenceInfoCallback {
        void onSuccess(ConferenceInfo info);

        void onFail(int code, String msg);
    }

    interface FavConferenceCallback {
        void onSuccess(List<ConferenceInfo> infos);

        void onFail(int code, String msg);
    }


}
