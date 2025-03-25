/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.service;

import static cn.wildfire.chat.kit.third.utils.UIUtils.getPackageName;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.group.GroupAnnouncement;
import cn.wildfire.chat.kit.net.BooleanCallback;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;
import okhttp3.MediaType;

public class IMService {
    private static final IMService Instance = new IMService();

    /**
     * App Server默认使用的是8888端口，替换为自己部署的服务时需要注意端口别填错了
     * <br>
     * 这是个 http 地址，http 前缀不能省略，否则会提示配置错误，然后直接退出
     * <br>
     * 正式商用时，建议用https，确保token安全
     * <br>
     * <br>
     */
    public static String RY_SERVER_ADDRESS/*请仔细阅读上面的注释*/ = "http://1.94.177.77:8080";

    private IMService() {

    }

    public static IMService Instance() {
        return Instance;
    }


    public void submitGroupApply(String groupId, String userId, String reason, GeneralCallback callback) {
        String url = RY_SERVER_ADDRESS + "/api/groupJoin";
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("applicantId", userId);
        params.put("remark", reason);

        OKHttpHelper.post(url, params, new SimpleCallback<BaseResponse>() {
            @Override
            public void onUiSuccess(BaseResponse response) {
                if (response.code == 200) {
                    callback.onSuccess();
                } else {
                    callback.onFail(response.code);
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                callback.onFail(code);
            }
        });
    }

}
