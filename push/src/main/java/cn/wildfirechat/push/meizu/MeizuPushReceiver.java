package cn.wildfirechat.push.meizu;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.meizu.cloud.pushsdk.MzPushMessageReceiver;
import com.meizu.cloud.pushsdk.platform.message.PushSwitchStatus;
import com.meizu.cloud.pushsdk.platform.message.RegisterStatus;
import com.meizu.cloud.pushsdk.platform.message.SubAliasStatus;
import com.meizu.cloud.pushsdk.platform.message.SubTagsStatus;
import com.meizu.cloud.pushsdk.platform.message.UnRegisterStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Set;

import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.push.AndroidPushMessage;
import cn.wildfirechat.push.PushService;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.PushType;

public class MeizuPushReceiver extends MzPushMessageReceiver {

    @Override
    public void onRegister(Context context, String pushId) {
        Log.e(TAG, "onReceiveClientId -> " + "pushId = " + pushId);
        try {
            ChatManager.Instance().setDeviceToken(pushId, PushType.MEIZU);
        } catch (NotInitializedExecption notInitializedExecption) {
            notInitializedExecption.printStackTrace();
        }
    }

    @Override
    public void onUnRegister(Context context, boolean success) {

    }

    @Override
    public void onPushStatus(Context context, PushSwitchStatus pushSwitchStatus) {
        Log.e(TAG, "onPS " + pushSwitchStatus);
    }

    @Override
    public void onRegisterStatus(Context context, RegisterStatus registerStatus) {
        Log.e(TAG, "onRS " + registerStatus);
    }

    @Override
    public void onUnRegisterStatus(Context context, UnRegisterStatus unRegisterStatus) {

    }

    @Override
    public void onSubTagsStatus(Context context, SubTagsStatus subTagsStatus) {

    }

    @Override
    public void onSubAliasStatus(Context context, SubAliasStatus subAliasStatus) {

    }

    @Override
    public void onNotificationClicked(Context context, String title, String content, String selfDefineContentString) {
        PushService.showMainActivity(context);
    }

    @Override
    public void onNotificationArrived(Context context, String title, String content, String selfDefineContentString) {
        super.onNotificationArrived(context, title, content, selfDefineContentString);
        Log.e(TAG, "onNA");
    }

    @Override
    public void onNotificationDeleted(Context context, String title, String content, String selfDefineContentString) {
        super.onNotificationDeleted(context, title, content, selfDefineContentString);
        Log.e(TAG, "onND");
    }

    @Override
    public void onNotifyMessageArrived(Context context, String message) {
        super.onNotifyMessageArrived(context, message);
        Log.e(TAG, "onNMA");
    }

    @Override
    public void onMessage(Context context, String message) {
        Log.d(TAG, "receiver push message = " + message);
        try {
            AndroidPushMessage pushMessage = AndroidPushMessage.messageFromJson(message);
            PushService.didReceiveIMPushMessage(context, pushMessage, PushService.PushServiceType.MeiZu);
        } catch (Exception e) {
            PushService.didReceivePushMessageData(context, message);
        }
        Log.d(TAG, "receiver payload = " + message);
    }

    @Override
    public void onMessage(Context context, Intent intent) {
        String json = toJson(intent);
        onMessage(context, json);
    }

    public String toJson(Intent intent) {
        if (intent == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            Bundle extras = intent.getExtras();
            Set<String> keySet = extras.keySet();
            Iterator<String> iterator = keySet.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                Object value = extras.get(next);
                if (value instanceof Boolean) {
                    jsonObject.put(next, (Boolean) value);
                } else if (value instanceof Integer) {
                    jsonObject.put(next, (Integer) value);
                } else if (value instanceof Long) {
                    jsonObject.put(next, (Long) value);
                } else if (value instanceof Double) {
                    jsonObject.put(next, (Double) value);
                } else if (value instanceof String) {
                    jsonObject.put(next, value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
