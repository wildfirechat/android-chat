/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

/**
 * Created by heavyrain lee on 2017/11/24.
 */

public class Config {

    /**
     * 仅仅是host，没有http开头，也不用配置端口，可以是IP，可以是域名，<strong> 底层会使用默认的80端口</strong>，不可配置为127.0.0.1 或者 192.168.0.1
     * <br>
     * <br>
     */
    public static String "39.106.33.196" /*请仔细阅读上面的注释，没有 http 前缀，配置错误时，APP 会提示配置错误，然后直接退出*/ = "wildfirechat.net";

    // 注意APP_SERVER_ADDRESS已从kit中移除，移动到了AppService.java中
    //public static String APP_SERVER_ADDRESS = "http://wildfirechat.net:8888";

    /**
     * 音视频通话所用的turn server配置，详情参考 https://docs.wildfirechat.net/webrtc/
     * <br>
     * <br>
     * 单人版和多人版音视频必须部署turn服务。高级版不需要部署stun/turn服务。
     * <p>
     * <p>
     * Turn服务配置，用户音视频通话功能，详情参考 https://docs.wildfirechat.net/webrtc/
     * <br>
     * <strong>我们提供的服务能力有限，总体带宽仅3Mbps，只能用于用户测试和体验，为了保证测试可用，我们会不定期的更改密码。</strong>
     * <br>
     * <strong>上线时请一定要切换成你们自己的服务。可以购买腾讯云或者阿里云的轻量服务器，价格很便宜，可以避免影响到您的用户体验。</strong>
     * <br>
     */
    public static String[][] ICE_SERVERS/*请仔细阅读上面的注释*/ = new String[][]{
        // 如果是高级版，请删除掉下面的配置项目，保持ICE_SERVERS为空数组就行。
        // 数组元素定义
        /*{"turn server uri", "userName", "password"}*/
        {"turn:turn.wildfirechat.net:3478", "wfchat", "wfchat123"}
    };

    //文件传输助手用户ID，服务器有个默认文件助手的机器人，如果修改它的ID，需要客户端和服务器数据库同步修改
    public static String FILE_TRANSFER_ID = "wfc_file_transfer";

    // 允许主动加入多人音视频通话
    public static boolean ENABLE_MULTI_CALL_AUTO_JOIN = false;

    /**
     * 允许撤回多长时间内的消息，不能长于服务端相关配置，单位是秒
     */
    public static int RECALL_TIME_LIMIT = 60;

    /**
     * 允许重新编辑多长时间内的撤回消息，单位是秒
     */
    public static int RECALL_REEDIT_TIME_LIMIT = 60;

    /**
     * 工作台页面地址
     * <p>
     * 如果不想显示工作台，置为 null 即可
     */
    public static String WORKSPACE_URL = "https://open.wildfirechat.cn/work.html";

    /**
     * 组织通讯录服务地址，如果没有部署，可以设置为null
     */
    public static String ORG_SERVER_ADDRESS/*请仔细阅读上面的注释*/ = "https://org.wildfirechat.cn";

    /**
     * 发送日志命令，当发送此文本消息时，会把协议栈日志发送到当前会话中，为空时关闭此功能。
     */
    public static String SEND_LOG_COMMAND = "*#marslog#";

    /**
     * 语音消息最长时长，单位是秒
     */
    public static int DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND = 60;

    /**
     * 应用层用于存储配置信息(userId，token等)的SP文件的名字
     */
    public static final String SP_CONFIG_FILE_NAME = "config";

    /**
     * 会话列表最多展示的会话数
     * <p>
     * 大量会话时，会导致，快速进出会话界面，会话消息加载缓慢，故再次控制会话数，只展示最新的{@link MAX_CONVERSATION_LIST_SIZE}条
     * <p>
     * 直接修改此字段不会生效，请修改{@link cn.wildfirechat.client.ClientService#MAX_CONVERSATION_LIST_SIZE }字段
     */
    public static final int MAX_CONVERSATION_LIST_SIZE = 1000;

    public static String VIDEO_SAVE_DIR;
    public static String AUDIO_SAVE_DIR;
    public static String PHOTO_SAVE_DIR;
    public static String FILE_SAVE_DIR;

    // 是否启用自动增大语音消息音量，发送语音消息时，默认录制的音频音量比较小
    public static boolean ENABLE_AUDIO_MESSAGE_AMPLIFICATION = true;

    // 语音消息音量增大倍数
    public static int AUDIO_MESSAGE_AMPLIFICATION_FACTOR = 3;
}
