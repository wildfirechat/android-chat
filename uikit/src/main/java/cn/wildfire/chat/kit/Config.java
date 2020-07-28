package cn.wildfire.chat.kit;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by heavyrain lee on 2017/11/24.
 */

public class Config {

    // 仅仅是host，没有http开头，也不用配置端口，底层会使用默认的80端口，不可配置为127.0.0.1 或者 192.168.0.1
    // 可以是IP，可以是域名，如果是域名的话只支持主域名或www域名或im的二级域名，其它二级域名不支持！建议使用域名。
    // 例如：example.com或www.example.com或im.example.com是支持的；xx.example.com或xx.yy.example.com是不支持的。
    public static String IM_SERVER_HOST = "wildfirechat.cn"; // 会固定使用到80端口，不可以配置端口!!!

    // App Server默认使用的是8888端口，替换为自己部署的服务时需要注意端口别填错了
    // 正式商用时，建议用https，确保token安全
    public static String APP_SERVER_ADDRESS = "http://wildfirechat.cn:8888";

    public static String ICE_ADDRESS = "turn:turn.wildfirechat.cn:3478";
    public static String ICE_ADDRESS2 = "turn:117.51.153.82:3478";
    public static String ICE_USERNAME = "wfchat";
    public static String ICE_PASSWORD = "wfchat";

    public static int DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND = 120;

    // 支持多人音视频时有效
    public static int MAX_VIDEO_PARTICIPANT_COUNT = 4;
    public static int MAX_AUDIO_PARTICIPANT_COUNT = 9;

    public static String VIDEO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/video";
    public static String AUDIO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/audio";
    public static String PHOTO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/photo";
    public static String FILE_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/file";

    public static void validateConfig() {
        if (TextUtils.isEmpty(IM_SERVER_HOST)
            || IM_SERVER_HOST.startsWith("http")
            || TextUtils.isEmpty(APP_SERVER_ADDRESS)
            || (!APP_SERVER_ADDRESS.startsWith("http") && !APP_SERVER_ADDRESS.startsWith("https"))
            || IM_SERVER_HOST.equals("127.0.0.1")
            || APP_SERVER_ADDRESS.contains("127.0.0.1")
        ) {
            throw new IllegalStateException("im server host config error");
        }

        if(Config.IM_SERVER_HOST.contains(":")){
            Log.e("WFC", "!!!!IM_SERVER_HOST不能配置端口!!!!");
        }
    }
}
