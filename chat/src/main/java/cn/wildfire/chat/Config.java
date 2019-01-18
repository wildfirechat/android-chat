package cn.wildfire.chat;

import cn.wildfire.chat.third.utils.FileUtils;

/**
 * Created by heavyrain lee on 2017/11/24.
 */

public interface Config {

    boolean USE_EMBED_APP_SERVER = true;

    String IM_SERVER_HOST = "192.168.31.113";
    int IM_SERVER_PORT = 8080;

    String INDEPENDENT_APP_SERVER_HOST = "192.168.31.113";
    int INDEPENDENT_APP_SERVER_PORT = 8888;

    String ICE_ADDRESS = "turn:turn.liyufan.win:3478";
    String ICE_USERNAME = "wfchat";
    String ICE_PASSWORD = "wfchat";

    int DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND = 120;
    String VIDEO_SAVE_DIR = FileUtils.getDir("video");
    String PHOTO_SAVE_DIR = FileUtils.getDir("photo");
    String HEADER_SAVE_DIR = FileUtils.getDir("header");
}
