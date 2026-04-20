/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

public class BaseApp extends MultiDexApplication {

    //以下属性应用于整个应用程序，合理利用资源，减少资源浪费
    private static Context mContext;//上下文
    private static long mMainThreadId;//主线程id
    private static Handler mHandler;//主线程Handler
    private int mLastNightMode;

    @Override
    public void onCreate() {
        super.onCreate();

        //对全局属性赋值
        mContext = getApplicationContext();
        mMainThreadId = android.os.Process.myTid();
        mHandler = new Handler();
        mLastNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int nightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode != mLastNightMode) {
            mLastNightMode = nightMode;
            // 显式切换 AppCompat 夜间模式，触发所有 Activity 重建
            AppCompatDelegate.setDefaultNightMode(
                nightMode == Configuration.UI_MODE_NIGHT_YES
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO
            );
        }
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
