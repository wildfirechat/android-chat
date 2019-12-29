package cn.wildfirechat.push.huawei;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.huawei.hms.support.api.push.PushReceiver;

import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.push.AndroidPushMessage;
import cn.wildfirechat.push.PushService;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.PushType;

/**
 * Created by heavyrainlee on 22/02/2018.
 */

public class HuaweiPushRevicer extends PushReceiver {
    private final static String TAG = "HuaweiPushRevicer";

    @Override
    public void onToken(Context context, String s, Bundle bundle) {
        try {
            ChatManager.Instance().setDeviceToken(s, PushType.HMS);
        } catch (NotInitializedExecption notInitializedExecption) {
            notInitializedExecption.printStackTrace();
        }
    }


    @Override
    public void onPushMsg(Context context, byte[] payload, String s) {
        String data = new String(payload);
        Log.d(TAG, "receiver payload = " + data);
        try {
            AndroidPushMessage pushMessage = AndroidPushMessage.messageFromJson(data);
            PushService.didReceiveIMPushMessage(context, pushMessage, PushService.PushServiceType.HMS);
        } catch (Exception e) {
            PushService.didReceivePushMessageData(context, data);
        }
    }

    @Override
    public void onEvent(Context context, Event event, Bundle extras) {
        if (event == Event.NOTIFICATION_OPENED || event == Event.NOTIFICATION_CLICK_BTN) {
            PushService.showMainActivity(context);
        } else {
            super.onEvent(context, event, extras);
        }
    }
}
