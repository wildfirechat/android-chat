package cn.wildfirechat.message.core;

/**
 * Created by heavyrain lee on 2017/12/6.
 */


//Content Type. 1000以下为系统内置类型，自定义消息需要使用1000以上
public interface MessageContentType {
    //基本消息类型
    int ContentType_Unknown = 0;
    int ContentType_Text = 1;
    int ContentType_Voice = 2;
    int ContentType_Image = 3;
    int ContentType_Location = 4;
    int ContentType_File = 5;
    int ContentType_Video = 6;
    int ContentType_Sticker = 7;
    int ContentType_ImageText = 8;

    int ContentType_Recall = 80;

    //提醒消息
    int ContentType_Tip_Notification = 90;

    //正在输入消息
    int ContentType_Typing = 91;

    //通知消息类型
    int ContentType_General_Notification = 100;
    int ContentType_CREATE_GROUP = 104;
    int ContentType_ADD_GROUP_MEMBER = 105;
    int ContentType_KICKOF_GROUP_MEMBER = 106;
    int ContentType_QUIT_GROUP = 107;
    int ContentType_DISMISS_GROUP = 108;
    int ContentType_TRANSFER_GROUP_OWNER = 109;


    int ContentType_CHANGE_GROUP_NAME = 110;
    int ContentType_MODIFY_GROUP_ALIAS = 111;
    int ContentType_CHANGE_GROUP_PORTRAIT = 112;

    int CONTENT_TYPE_CHANGE_MUTE = 113;
    int CONTENT_TYPE_CHANGE_JOINTYPE = 114;
    int CONTENT_TYPE_CHANGE_PRIVATECHAT = 115;
    int CONTENT_TYPE_CHANGE_SEARCHABLE = 116;
    int CONTENT_TYPE_SET_MANAGER = 117;

    int ContentType_Call_Start = 400;
    int ContentType_Call_End = 402;
    int ContentType_Call_Accept = 401;
    int ContentType_Call_Signal = 403;
    int ContentType_Call_Modify = 404;
    int ContentType_Call_Accept_T = 405;

    //自定义消息type要做1000以上
}
