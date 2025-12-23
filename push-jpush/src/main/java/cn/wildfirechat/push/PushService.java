/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.push;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import cn.jpush.android.api.JPushInterface;

/**
 * Created by heavyrain.lee on 2018/2/26.
 */

public class PushService {
    private int pushServiceType;
    private static PushService INST = new PushService();

    public static final String TAG = "PushService";

    public interface PushServiceType {
        int Unknown = 0;
        int Xiaomi = 1;
        int HMS = 2;
        int MeiZu = 3;
        int VIVO = 4;
        int OPPO = 5;
        int Google = 6;
        int GeTui = 7;
        int JPush = 8;
    }

    public static void init(Application gContext, String applicationId) {
        INST.pushServiceType = PushServiceType.JPush;
        INST.initJPush(gContext);

//        try {
//            com.igexin.sdk.PushManager.getInstance().checkManifest(gContext);
//        } catch (GetuiPushException e) {
//            Log.e(TAG, e.getLocalizedMessage());
//            e.printStackTrace();
//        }

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onForeground() {
                clearNotification(gContext);
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onBackground() {
            }
        });

    }

    private static void clearNotification(Context context) {
        // TODO
    }

    private void initJPush(Context context) {
        JPushInterface.setDebugMode(true);
        JPushInterface.init(context);
        String registerId = JPushInterface.getRegistrationID(context);
        Log.d(TAG, "JPush registerId: " + registerId);
    }

    public static int getPushServiceType() {
        return INST.pushServiceType;
    }
}
