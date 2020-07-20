package cn.wildfire.chat.app;

import android.app.ActivityManager;
import android.content.Context;

import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversation.message.viewholder.MessageViewHolderManager;
import cn.wildfire.chat.kit.third.location.viewholder.LocationMessageContentViewHolder;
import cn.wildfirechat.chat.app.BuildConfig;
import cn.wildfirechat.chat.app.R;
import cn.wildfirechat.push.PushService;


public class MyApp extends BaseApp {

    @Override
    public void onCreate() {
        super.onCreate();
        Config.validateConfig();

        // bugly，务必替换为你自己的!!!
        if ("wildfirechat.cn".equals(Config.IM_SERVER_HOST)) {
            CrashReport.initCrashReport(getApplicationContext(), Config.BUGLY_ID, false);
        }
        // 只在主进程初始化，否则会导致重复收到消息
        if (getCurProcessName(this).equals(BuildConfig.APPLICATION_ID)) {
            WfcUIKit wfcUIKit = WfcUIKit.getWfcUIKit();
            wfcUIKit.init(this);
            wfcUIKit.setAppServiceProvider(AppService.Instance());
            PushService.init(this, BuildConfig.APPLICATION_ID);
            MessageViewHolderManager.getInstance().registerMessageViewHolder(LocationMessageContentViewHolder.class, R.layout.conversation_item_location_send, R.layout.conversation_item_location_send);
            setupWFCDirs();
        }
    }

    private void setupWFCDirs() {
        File file = new File(Config.VIDEO_SAVE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(Config.AUDIO_SAVE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(Config.FILE_SAVE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(Config.PHOTO_SAVE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static String getCurProcessName(Context context) {

        int pid = android.os.Process.myPid();

        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager
                .getRunningAppProcesses()) {

            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }
}
