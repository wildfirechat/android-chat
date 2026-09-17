/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

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
    // 缓存的是生成头像的请求参数，不缓存完整地址，双网切换网络后，应用服务地址会变
    private final Map<String, Pair<Long,  String>> groupPortraitRequestMap = new HashMap<>();

    @Override
    public String userDefaultPortrait(UserInfo userInfo) {
        if (!TextUtils.isEmpty(userInfo.portrait)) {
            return userInfo.portrait;
        } else {
            return AppService.Instance().appServerAddress() + "/avatar?name=" + Uri.encode(userInfo.displayName);
        }
    }

    @Override
    public String groupDefaultPortrait(GroupInfo groupInfo, List<UserInfo> userInfos) {
        if (groupInfo instanceof NullGroupInfo || !TextUtils.isEmpty(groupInfo.portrait) || userInfos == null || userInfos.isEmpty()) {
            return groupInfo.portrait;
        }
        Pair<Long, String> pair = groupPortraitRequestMap.get(groupInfo.target);
        if (pair != null && pair.first == groupInfo.updateDt) {
            return groupPortraitUrl(pair.second);
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
                if (TextUtils.isEmpty(userInfo.portrait) || AppService.isAppServerUrl(userInfo.portrait)) {
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
        groupPortraitRequestMap.put(groupInfo.target, new Pair<>(groupInfo.updateDt, request.toString()));
        return groupPortraitUrl(request.toString());
    }

    private String groupPortraitUrl(String request) {
        return AppService.Instance().appServerAddress() + "/avatar/group?request=" + Uri.encode(request);
    }
}
