package cn.wildfire.chat.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by heavyrain lee on 2017/11/24.
 */

public interface Config {
    // 仅仅是host，没有http开头，不可配置为127.0.0.1 或者 192.168.0.1
    // host可以是IP，可以是域名，如果是域名的话只支持主域名或www域名，二级域名不支持！
    // 例如：example.com或www.example.com是支持的；xx.example.com或xx.yy.example.com是不支持的。

    /***/
    static  String getMFSU(){
        ApplicationInfo info;
        try {
            //获取包管理器
            PackageManager pm = MyApp._context.getPackageManager();
            info = pm.getApplicationInfo(
                    MyApp._context.getPackageName(), PackageManager.GET_META_DATA);
            String flag = (String) info.metaData.get("serverurl");
            return flag;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    static  String getMFSH(){
        ApplicationInfo info;
        try {
            //获取包管理器
            PackageManager pm = MyApp._context.getPackageManager();
            info = pm.getApplicationInfo(
                    MyApp._context.getPackageName(), PackageManager.GET_META_DATA);
            String flag = (String) info.metaData.get("serverhttp");
            return flag;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    static  String getMFSPort(){
        ApplicationInfo info;
        try {
            //获取包管理器
            PackageManager pm = MyApp._context.getPackageManager();
            info = pm.getApplicationInfo(
                    MyApp._context.getPackageName(), PackageManager.GET_META_DATA);
            int _flag = (int)info.metaData.get("serverport");
            String flag = Integer.toString(_flag);
            return flag;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    //String IM_SERVER_HOST = "192.168.133.179";
    String IM_SERVER_HOST = getMFSU();

    //客户端强制使用80端口，不能使用其它端口。需要确保服务器运行在在国内时处于备案状态，确保运营IM服务处在监管之下。
    //int IM_SERVER_PORT = 80;

    //正式商用时，建议用https，确保token安全
    //String APP_SERVER_URL = "http://192.168.133.179";
    String APP_SERVER_URL = "http://"+getMFSU();
    String APP_SERVER_ADDRESS = APP_SERVER_URL + ":8888";
    String APP_SERVER_PHP = getMFSH()+"://"+getMFSU() + ":" + getMFSPort();

    String ICE_ADDRESS = "turn:turn.wildfirechat.cn:3478";
    String ICE_USERNAME = "wfchat";
    String ICE_PASSWORD = "wfchat";
    // 二次开发时，一定记得替换为你们自己的
    String BUGLY_ID = "34490ba79f";

    int DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND = 120;

    String VIDEO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/video";
    String AUDIO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/audio";
    String PHOTO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/photo";
    String FILE_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/file";

    static void validateConfig() {
        if (TextUtils.isEmpty(IM_SERVER_HOST)
                || IM_SERVER_HOST.startsWith("http")
                || TextUtils.isEmpty(APP_SERVER_ADDRESS)
                || (!APP_SERVER_ADDRESS.startsWith("http") && !APP_SERVER_ADDRESS.startsWith("https"))
                || IM_SERVER_HOST.equals("127.0.0.1")
                || APP_SERVER_ADDRESS.contains("127.0.0.1")
        ) {
            throw new IllegalStateException("im server host config error");
        }

        if (!IM_SERVER_HOST.equals("wildfirechat.cn")) {
            if ("34490ba79f".equals(BUGLY_ID)) {
                Log.e("wfc config", "二次开发一定需要将buglyId替换为自己的!!!!1");
            }
        }
    }
}
