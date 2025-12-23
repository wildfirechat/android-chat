/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.push.honor;

import android.util.Log;

import com.hihonor.push.sdk.HonorMessageService;
import com.hihonor.push.sdk.HonorPushDataMsg;

import cn.wildfirechat.push.PushService;
import cn.wildfirechat.remote.ChatManager;

public class HonorPushService extends HonorMessageService {
    private static final String TAG = "PushService";

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.d(TAG, "honor onNewToken: " + s);
        ChatManager.Instance().setDeviceToken(s, PushService.PushServiceType.HMS);
    }

    @Override
    public void onMessageReceived(HonorPushDataMsg honorPushDataMsg) {
        super.onMessageReceived(honorPushDataMsg);
        // do nothing
    }
}
