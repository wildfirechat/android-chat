/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.push.getui;

import android.content.Context;
import android.util.Log;

import com.igexin.sdk.GTIntentService;

public class GetuiIntentService extends GTIntentService {
    private static final String TAG = "Getui";

    @Override
    public void onReceiveClientId(Context context, String s) {
        Log.d(TAG, "onReceiveClientId: " + s);
    }
}
