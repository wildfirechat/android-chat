package cn.wildfirechat.push;

import android.content.Context;

/**
 * Created by heavyrain.lee on 2018/3/18.
 */

public abstract class PushMessageHandler {
    /*
    如果需要自定义push的展示，请实现CUSTOM_PUSH_MSG_HANDLER类，并继承PushMessageHandler。构造函数为默认构造函数
     */
    public static final String CUSTOM_PUSH_MSG_HANDLER = "cn.wildfirechat.push.CustomPushMessageHandler";

    public static final int DEFAULT_NOTIFICATION_ID = 1883;
    public static final int DEFAULT_NOTIFICATION_CALL_ID = 1884;
    private static PushMessageHandler customHandler;
    private static PushMessageHandler defaultHandler;

    static {
        defaultHandler = new DefaultPushMessageHandler();
        try {
            Class cls = Class.forName(CUSTOM_PUSH_MSG_HANDLER);
            customHandler = (PushMessageHandler) cls.newInstance();
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
        } catch (InstantiationException e) {
            //e.printStackTrace();
        } catch (IllegalAccessException e) {
            //e.printStackTrace();
        }
    }

    //IM push message
    abstract public void handleIMPushMessage(Context context, AndroidPushMessage pushMessage, PushService.PushServiceType pushServiceType);

    //Application push data
    abstract public void handlePushMessageData(Context context, String pushData);


    public static void didReceiveIMPushMessage(final Context context, final AndroidPushMessage pushMessage, final PushService.PushServiceType pushServiceType) {
        if (customHandler != null) {
            customHandler.handleIMPushMessage(context, pushMessage, pushServiceType);
        } else if (defaultHandler != null) {
            defaultHandler.handleIMPushMessage(context, pushMessage, pushServiceType);
        }
    }


    public static void didReceivePushMessageData(Context context, String pushData) {
        if (customHandler != null) {
            customHandler.handlePushMessageData(context, pushData);
        } else if (defaultHandler != null) {
            defaultHandler.handlePushMessageData(context, pushData);
        }
    }

}
