package cn.wildfire.chat.kit;

public interface WfcIntent {
    String ACTION_CONVERSATION = BuildConfig.LIBRARY_PACKAGE_NAME + ".conversation";
    String ACTION_CONTACT = BuildConfig.LIBRARY_PACKAGE_NAME + ".contact";
    String ACTION_USER_INFO = BuildConfig.LIBRARY_PACKAGE_NAME + ".user.info";
    String ACTION_GROUP_INFO = BuildConfig.LIBRARY_PACKAGE_NAME + ".group.info";
    String ACTION_VOIP_SINGLE = BuildConfig.LIBRARY_PACKAGE_NAME + ".kit.voip.single";
    String ACTION_VOIP_MULTI = BuildConfig.LIBRARY_PACKAGE_NAME + ".kit.voip.multi";
    String ACTION_VIEW = BuildConfig.LIBRARY_PACKAGE_NAME + ".webview";

    String ACTION_MOMENT = BuildConfig.LIBRARY_PACKAGE_NAME + ".moment";
}
