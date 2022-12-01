/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.push.getui;

import android.content.Context;
import android.util.Log;

import com.igexin.sdk.GTIntentService;

import cn.wildfirechat.push.PushService;
import cn.wildfirechat.remote.ChatManager;

public class GetuiIntentService extends GTIntentService {
    private static final String TAG = "Getui";

    @Override
    public void onReceiveClientId(Context context, String s) {
        Log.d(TAG, "onReceiveClientId: " + s);
        ChatManager.Instance().setDeviceToken(s, PushService.PushServiceType.Getui.ordinal());
    }
}
