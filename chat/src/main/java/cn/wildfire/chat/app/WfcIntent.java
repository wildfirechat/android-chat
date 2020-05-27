package cn.wildfire.chat.app;

import cn.wildfirechat.chat.BuildConfig;

public interface WfcIntent {
    String ACTION_MAIN = BuildConfig.APPLICATION_ID + ".main";
    String ACTION_CONVERSATION = BuildConfig.APPLICATION_ID + ".conversation";
    String ACTION_CONTACT = BuildConfig.APPLICATION_ID + ".contact";
    String ACTION_USER_INFO = BuildConfig.APPLICATION_ID + ".user.info";
    String ACTION_GROUP_INFO = BuildConfig.APPLICATION_ID + ".group.info";
    String ACTION_VOIP_SINGLE = BuildConfig.APPLICATION_ID + ".kit.voip.single";
    String ACTION_VOIP_MULTI = BuildConfig.APPLICATION_ID + ".kit.voip.multi";
    String ACTION_VIEW = BuildConfig.APPLICATION_ID + ".webview";

    String ACTION_MOMENT = BuildConfig.APPLICATION_ID + ".moment";
}
