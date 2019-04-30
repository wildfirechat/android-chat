package cn.wildfire.chat.app;

import android.os.Environment;

/**
 * Created by heavyrain lee on 2017/11/24.
 */

public interface Config {

    String IM_SERVER_HOST = "wildfirechat.cn";
    int IM_SERVER_PORT = 80;

    String APP_SERVER_HOST = "wildfirechat.cn";
    int APP_SERVER_PORT = 8888;

    String ICE_ADDRESS = "turn:turn.liyufan.win:3478";
    String ICE_USERNAME = "wfchat";
    String ICE_PASSWORD = "wfchat";

    int DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND = 120;

    String SP_NAME = "config";
    String SP_KEY_SHOW_GROUP_MEMBER_ALIAS = "show_group_member_alias:%s";

    String VIDEO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/video";
    String AUDIO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/audio";
    String PHOTO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/photo";
    String FILE_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/file";
}
