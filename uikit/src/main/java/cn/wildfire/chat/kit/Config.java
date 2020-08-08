package cn.wildfire.chat.kit;

import android.os.Environment;

/**
 * Created by heavyrain lee on 2017/11/24.
 */

public class Config<ICE_SERVERS> {

    /**
     * 仅仅是host，没有http开头，也不用配置端口，<strong> 底层会使用默认的80端口</strong>，不可配置为127.0.0.1 或者 192.168.0.1
     * <br>
     * <br>
     * 可以是IP，可以是域名，如果是域名的话只支持主域名或www域名或im或imtest或imdev的二级域名，其它二级域名不支持！建议使用域名。
     * <br>
     * <br>
     * 例如：example.com或www.example.com或im.example.com是支持的；xx.example.com或xx.yy.example.com是不支持的。
     * <br>
     * <br>
     */
    public static String IM_SERVER_HOST = "wildfirechat.cn";

    // 注意APP_SERVER_ADDRESS已从kit中移除，移动到了AppService中
    //public static String APP_SERVER_ADDRESS = "http://wildfirechat.cn:8888";

    public static String[][] ICE_SERVERS = new String[][]{
        // 数组元素定义
        /*{"turn server uri", "userName", "password"}*/
        {"turn:turn.wildfirechat.cn:3478", "wfchat", "wfchat"},
        {"turn:117.51.153.82:3478", "wfchat", "wfchat"}
    };

    public static int DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND = 120;

    // 支持多人音视频时有效
    public static int MAX_VIDEO_PARTICIPANT_COUNT = 4;
    public static int MAX_AUDIO_PARTICIPANT_COUNT = 9;

    public static String VIDEO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/video";
    public static String AUDIO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/audio";
    public static String PHOTO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/photo";
    public static String FILE_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/file";

}
