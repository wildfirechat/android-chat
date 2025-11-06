/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import android.content.Context;
import android.os.Handler;

import androidx.multidex.MultiDexApplication;

public class BaseApp extends MultiDexApplication {

    // The following properties apply to the entire application, to make reasonable use of resources and reduce resource waste
    private static Context mContext; // Context
    private static long mMainThreadId; // Main thread ID
    private static Handler mHandler; // Main thread Handler

    @Override
    public void onCreate() {
        super.onCreate();

        // Assign values to global properties
        mContext = getApplicationContext();
        mMainThreadId = android.os.Process.myTid();
        mHandler = new Handler();
    }

    public static Context getContext() {
        return mContext;
    }

    public static void setContext(Context mContext) {
        BaseApp.mContext = mContext;
    }

    public static long getMainThreadId() {
        return mMainThreadId;
    }

    public static Handler getMainHandler() {
        return mHandler;
    }
}
