/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.NullGroupInfo;
import cn.wildfirechat.model.NullUserInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.DefaultPortraitProvider;

public class WfcDefaultPortraitProvider implements DefaultPortraitProvider {
    private final Map<String, String> groupPortraitMap = new HashMap<>();

    @Override
    public String userDefaultPortrait(UserInfo userInfo) {
        if (!TextUtils.isEmpty(userInfo.portrait)) {
            return userInfo.portrait;
        } else {
            return AppService.APP_SERVER_ADDRESS + "/avatar?name=" + Uri.encode(userInfo.displayName);
        }
    }

    @Override
    public String groupDefaultPortrait(GroupInfo groupInfo, List<UserInfo> userInfos) {
        if (groupInfo instanceof NullGroupInfo || !TextUtils.isEmpty(groupInfo.portrait) || userInfos == null || userInfos.isEmpty()) {
            return groupInfo.portrait;
        }
        String portrait = groupPortraitMap.get(groupInfo.target);
        if (portrait != null) {
            return portrait;
        }

        boolean pending = false;
        JSONObject request = new JSONObject();
        try {
            JSONArray reqMembers = new JSONArray();
            if (userInfos.size() > 9) {
                userInfos = userInfos.subList(0, 9);
            }
            for (UserInfo userInfo : userInfos) {
                if (userInfo instanceof NullUserInfo) {
                    pending = true;
                }
                JSONObject obj = new JSONObject();
                if (TextUtils.isEmpty(userInfo.portrait) || userInfo.portrait.startsWith(AppService.APP_SERVER_ADDRESS)) {
                    obj.put("name", userInfo.displayName);
                } else {
                    obj.put("avatarUrl", userInfo.portrait);
                }
                reqMembers.put(obj);
            }
            request.put("members", reqMembers);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (pending) {
            return null;
        }
        portrait = AppService.APP_SERVER_ADDRESS + "/avatar/group?request=" + Uri.encode(request.toString());
        groupPortraitMap.put(groupInfo.target, portrait);
        return portrait;
    }
}
