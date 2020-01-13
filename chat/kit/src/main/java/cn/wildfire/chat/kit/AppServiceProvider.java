package cn.wildfire.chat.kit;

import android.app.Application;

import cn.wildfire.chat.app.AppService;
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

    public void getGroupAnnouncement(String groupId, AppService.GetGroupAnnouncementCallback callback);

    public void updateGroupAnnouncement(String groupId, String announcement, AppService.UpdateGroupAnnouncementCallback callback);

    /**
     * 前置条件是已经调过{@link cn.wildfirechat.remote.ChatManager#init(Application, String)}
     *
     * @param callback
     */
    void uploadLog(SimpleCallback<String> callback);
}
