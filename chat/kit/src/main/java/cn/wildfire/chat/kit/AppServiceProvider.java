package cn.wildfire.chat.kit;

import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.kit.group.GroupAnnouncement;
import cn.wildfire.chat.kit.net.OKHttpHelper;
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
}
