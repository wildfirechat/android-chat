/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.push.huawei;

import android.util.Log;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

import cn.wildfirechat.push.PushService;
import cn.wildfirechat.remote.ChatManager;

public class HwPushService extends HmsMessageService {
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.d("HMS", "onNewToken: " + s);
        ChatManager.Instance().setDeviceToken(s, PushService.PushServiceType.HMS);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("HMS", "receiveMessage");
        // do nothing
        // 悟聊采用的是透传，只需将主进程拉起即可，主进程会去拉取消息，并显示通知
        // 手机设置：
        // 1. 应用权限管理里面，需要允许自启动、运行后台活动
        // 2. 通知管理，允许通知，在状态栏显示，横幅，允许打扰
    }
}
