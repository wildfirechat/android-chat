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

import com.igexin.sdk.IUserLoggerInterface;

/**
 * Created by heavyrain.lee on 2018/2/26.
 */

public class PushService {
    private PushServiceType pushServiceType;
    private static PushService INST = new PushService();

    public static final String TAG = "PushService";

    public enum PushServiceType {
        Unknown, Xiaomi, HMS, MeiZu, VIVO, OPPO, Google, Getui
    }

    public static void init(Application gContext, String applicationId) {
        INST.pushServiceType = PushServiceType.Getui;
        INST.initGetui(gContext);

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

    private void initGetui(Context context) {
        com.igexin.sdk.PushManager.getInstance().initialize(context);
        com.igexin.sdk.PushManager.getInstance().setDebugLogger(context, new IUserLoggerInterface() {
            @Override
            public void log(String s) {
                Log.i(TAG, s);
            }
        });
    }

    public static PushServiceType getPushServiceType() {
        return INST.pushServiceType;
    }
}
