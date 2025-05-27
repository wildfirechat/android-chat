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

    // 一定记得替换为你们自己的，ID请从BUGLY官网申请。关于BUGLY，可以从BUGLY官网了解，或者百度。
    public static String BUGLY_ID = "15dfd5f6d1";

    public static String routeHost;
    public static int routePort;

    public static String longLinkHost;

    @Override
    public void onCreate() {
        super.onCreate();
        AppService.validateConfig(this);

        // bugly，务必替换为你自己的!!!
        if ("wildfirechat.net".equals(Config.IM_SERVER_HOST)) {
//            CrashReport.initCrashReport(getApplicationContext(), BUGLY_ID, false);
        }
        // 只在主进程初始化，否则会导致重复收到消息
        if (getCurProcessName(this).equals(BuildConfig.APPLICATION_ID)) {
            // 如果uikit是以aar的方式引入 ，那么需要在此对Config里面的属性进行配置，如：
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
                //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
                //另外不能多次connect，如果需要切换用户请先disconnect，然后3秒钟之后再connect（如果是用户手动登录可以不用等，因为用户操作很难3秒完成，如果程序自动切换请等3秒）
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
        // 获取用户语言偏好
        String language = LocaleUtils.getLanguage(base);
        // 应用用户语言设置
        Context context = LocaleUtils.updateResources(base, language);
        // 调用父类方法
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
