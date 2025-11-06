/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import java.lang.reflect.Method;

import cn.wildfire.chat.app.misc.KeyStoreUtil;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversation.message.viewholder.MessageViewHolderManager;
import cn.wildfire.chat.kit.third.location.viewholder.LocationMessageContentViewHolder;
import cn.wildfire.chat.kit.utils.LocaleUtils;
import cn.wildfirechat.chat.BuildConfig;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.push.PushService;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnConnectToServerListener;

public class MyApp extends BaseApp implements OnConnectToServerListener {

    // Be sure to replace this with your own. ID should be obtained from the BUGLY official website. For information about BUGLY, you can check the BUGLY official website or search online.
    public static String BUGLY_ID = "15dfd5f6d1";

    public static String routeHost;
    public static int routePort;

    public static String longLinkHost;

    @Override
    public void onCreate() {
        super.onCreate();
        AppService.validateConfig(this);

        // bugly, must replace with your own!!!
        if ("wildfirechat.net".equals(Config.IM_SERVER_HOST)) {
//            CrashReport.initCrashReport(getApplicationContext(), BUGLY_ID, false);
        }
        // Only initialize in the main process, otherwise it will cause duplicate message reception
        if (getCurProcessName(this).equals(BuildConfig.APPLICATION_ID)) {
            // If uikit is included as an aar, then you need to configure the properties in Config here, such as:
            // Config.IM_SERVER_HOST = "im.example.com";
            WfcUIKit wfcUIKit = WfcUIKit.getWfcUIKit();
            wfcUIKit.init(this);
            wfcUIKit.setEnableNativeNotification(true);
            wfcUIKit.setAppServiceProvider(AppService.Instance());
            PushService.init(this, BuildConfig.APPLICATION_ID);
            MessageViewHolderManager.getInstance().registerMessageViewHolder(LocationMessageContentViewHolder.class, R.layout.conversation_item_location_send, R.layout.conversation_item_location_send);

            String id = null;
            String token = null;
            try {
                id = KeyStoreUtil.getData(this, "wf_userId");
                token = KeyStoreUtil.getData(this, "wf_token");
                if (TextUtils.isEmpty(id) || TextUtils.isEmpty(token)) {
                    SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
                    id = sp.getString("id", null);
                    token = sp.getString("token", null);
                    if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(token)) {
                        KeyStoreUtil.saveData(this, "wf_userId", id);
                        KeyStoreUtil.saveData(this, "wf_token", token);
                        sp.edit().remove("id").remove("token").commit();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(token)) {
                // Note that token is strongly dependent on clientId. You must call getClientId to get the clientId, and then use this clientId to get the token. Only then can connect succeed. If you randomly use a clientId to get a token, the connection will fail.
                // Also, do not connect multiple times. If you need to switch users, disconnect first, then wait 3 seconds before connecting again (if it's a user manual login, you don't need to wait, because user operations are unlikely to complete in 3 seconds; if the program switches automatically, wait 3 seconds)
                ChatManagerHolder.gChatManager.connect(id, token);
            }

            if (!TextUtils.isEmpty(Config.ORG_SERVER_ADDRESS)) {
                OrganizationService organizationService = OrganizationService.Instance();
                wfcUIKit.setOrganizationServiceProvider(organizationService);
            }

            ChatManager.Instance().setDefaultPortraitProviderClazz(WfcDefaultPortraitProvider.class);
            ChatManager.Instance().setUrlRedirectorClazz(TestUrlRedirector.class);
            ChatManager.Instance().addConnectToServerListener(this);
            SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
            Config.ENABLE_AUDIO_MESSAGE_AMPLIFICATION = sp.getBoolean("audioMessageAmplificationEnabled", Config.ENABLE_AUDIO_MESSAGE_AMPLIFICATION);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        // Get user language preference
        String language = LocaleUtils.getLanguage(base);
        // Apply user language setting
        Context context = LocaleUtils.updateResources(base, language);
        // Call parent method
        super.attachBaseContext(context);
    }

    public static String getCurProcessName(Context context) {
        if (Build.VERSION.SDK_INT >= 28)
            return Application.getProcessName();

        // Using the same technique as Application.getProcessName() for older devices
        // Using reflection since ActivityThread is an internal API

        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThread = Class.forName("android.app.ActivityThread");

            // Before API 18, the method was incorrectly named "currentPackageName", but it still returned the process name
            // See https://github.com/aosp-mirror/platform_frameworks_base/commit/b57a50bd16ce25db441da5c1b63d48721bb90687
            String methodName = Build.VERSION.SDK_INT >= 18 ? "currentProcessName" : "currentPackageName";

            Method getProcessName = activityThread.getDeclaredMethod(methodName);
            return (String) getProcessName.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void onConnectToServer(String host, String ip, int port) {
        if (TextUtils.isEmpty(ip)) {
            routeHost = host;
            routePort = port;
        } else {
            longLinkHost = host;
        }
    }
}
