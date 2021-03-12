package cn.wildfirechat.push.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;

import cn.wildfirechat.push.AndroidPushMessage;
import cn.wildfirechat.push.PushService;
import cn.wildfirechat.remote.ChatManager;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        ChatManager.Instance().setDeviceToken(s, PushService.PushServiceType.Google.ordinal());
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("FCM", "receiveMessage");
        // do nothing
        // 只需将主进程拉起即可，主进程会去拉取消息，并显示通知
    }
}
