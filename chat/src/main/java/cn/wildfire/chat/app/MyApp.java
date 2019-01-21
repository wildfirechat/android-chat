package cn.wildfire.chat.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.lqr.emoji.LQREmotionKit;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import cn.wildfire.chat.ChatManagerHolder;
import cn.wildfire.chat.Config;
import cn.wildfire.chat.conversation.ConversationActivity;
import cn.wildfire.chat.main.MainActivity;
import cn.wildfire.chat.voip.AsyncPlayer;
import cn.wildfire.chat.voip.SingleVoipCallActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.push.PushService;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;

import static androidx.core.app.NotificationCompat.CATEGORY_MESSAGE;
import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static cn.wildfirechat.message.core.PersistFlag.Persist_And_Count;
import static cn.wildfirechat.model.Conversation.ConversationType.Group;
import static cn.wildfirechat.model.Conversation.ConversationType.Single;


public class MyApp extends BaseApp implements AVEngineKit.AVEngineCallback, OnReceiveMessageListener, OnRecallMessageListener {

    private boolean isBackground = true;

    @Override
    public void onCreate() {
        super.onCreate();

        if (getCurProcessName(this).equals("cn.wildfirechat.chat")) {
            initWFClient();
            //初始化表情控件
            LQREmotionKit.init(this, (context, path, imageView) -> Glide.with(context).load(path).apply(new RequestOptions().centerCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE)).into(imageView));

            ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_START)
                public void onForeground() {
                    isBackground = false;
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
                public void onBackground() {
                    isBackground = true;
                }
            });
        }
    }

    private void initWFClient() {
        ChatManager.init(this, Config.IM_SERVER_HOST, Config.IM_SERVER_PORT);
        try {
            ChatManagerHolder.gChatManager = ChatManager.Instance();
            ChatManagerHolder.gChatManager.startLog();
            ChatManagerHolder.gChatManager.addOnReceiveMessageListener(this);
            PushService.init(getApplicationContext());

            ringPlayer = new AsyncPlayer(null);
            AVEngineKit.init(getApplicationContext(), this);
            ChatManagerHolder.gAVEngine = AVEngineKit.Instance();
            ChatManagerHolder.gAVEngine.addIceServer(Config.ICE_ADDRESS, Config.ICE_USERNAME, Config.ICE_PASSWORD);

            SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
            String id = sp.getString("id", null);
            String token = sp.getString("token", null);
            if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(token)) {
                ChatManagerHolder.gChatManager.connect(id, token);
            }
        } catch (NotInitializedExecption notInitializedExecption) {
            notInitializedExecption.printStackTrace();
        }
    }

    public static String getCurProcessName(Context context) {

        int pid = android.os.Process.myPid();

        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager
                .getRunningAppProcesses()) {

            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    @Override
    public void onReceiveCall(AVEngineKit.CallSession session) {
        onCall(getApplicationContext(), session.getClientId(), false, session.isAudioOnly());
    }

    private AsyncPlayer ringPlayer;

    @Override
    public void shouldStartRing(boolean isIncomming) {
        if (isIncomming) {
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.incoming_call_ring);
            ringPlayer.play(getApplicationContext(), uri, true, AudioManager.STREAM_RING);
        } else {
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.outgoing_call_ring);
            ringPlayer.play(getApplicationContext(), uri, true, AudioManager.STREAM_RING);
        }
    }

    @Override
    public void shouldSopRing() {
        ringPlayer.stop();
    }

    public static void onCall(Context context, String targetId, boolean isMo, boolean isAudioOnly) {
        Intent intent = new Intent(context, SingleVoipCallActivity.class);
        intent.putExtra(SingleVoipCallActivity.EXTRA_MO, isMo);
        intent.putExtra(SingleVoipCallActivity.EXTRA_TARGET, targetId);
        intent.putExtra(SingleVoipCallActivity.EXTRA_AUDIO_ONLY, isAudioOnly);

        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    public void onReceive(List<Message> messages, boolean hasMore) {
        if (isBackground) {
            // FIXME: 2018/5/28 只是临时方案，No_Persist消息，我觉得不应当到这儿，注册监听时，
            // 就表明自己关系哪些类型的消息, 设置哪些种类的消息

            if (messages == null) {
                return;
            }
            long now = System.currentTimeMillis();
            long delta = ChatManager.Instance().getServerDeltaTime();
            Iterator<Message> iterator = messages.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
                if (message.content.getPersistFlag() == PersistFlag.No_Persist
                        || now - (message.serverTime - delta) > 10 * 1000) {
                    iterator.remove();
                }
            }
            handleIMPushMessage(this, messages);
        } else {
            // do nothing
        }
    }

    @Override
    public void onRecallMessage(Message message) {
        //todo cancel notification?
    }

    private void handleIMPushMessage(Context context, List<Message> messages) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "wildfirechat_msg";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /* Create or update. */
            NotificationChannel channel = new NotificationChannel(channelId,
                    "wildfire chat message",
                    NotificationManager.IMPORTANCE_DEFAULT);

            channel.enableLights(true); //是否在桌面icon右上角展示小红点
            channel.setLightColor(Color.GREEN); //小红点颜色
            channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
            notificationManager.createNotificationChannel(channel);
        }


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setAutoCancel(true)
                .setCategory(CATEGORY_MESSAGE)
                .setDefaults(DEFAULT_ALL);


        if (messages.size() > 1) {
            builder.setContentTitle("新消息");

            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            Message lastCountMessage = null;
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                if (message.direction == MessageDirection.Receive && message.content.getPersistFlag() == Persist_And_Count) {
                    lastCountMessage = message;
                    break;
                }
            }

            if (lastCountMessage != null) {
                String pushContent = lastCountMessage.content.encode().pushContent;
                if (pushContent == null) {
                    if (lastCountMessage.content.getType() == MessageContentType.ContentType_Text) {
                        TextMessageContent textMessageContent = (TextMessageContent) lastCountMessage.content;
                        pushContent = textMessageContent.getContent();
                    } else if (lastCountMessage.content.getType() == MessageContentType.ContentType_Image) {
                        pushContent = "[图片]";
                    } else if (lastCountMessage.content.getType() == MessageContentType.ContentType_Voice) {
                        pushContent = "[语音]";
                    }
                }
                if (pushContent != null) {
                    int unreadCount = ChatManager.Instance().getUnreadCountEx(Arrays.asList(Single, Group), Arrays.asList(0, 1, 2)).unread;
                    if (unreadCount > 1) {
                        pushContent = "[" + unreadCount + "条]" + pushContent;
                    }
                }

                builder.setContentText(pushContent);
                notificationManager.notify(1883, builder.build());
            }
        } else if (messages.size() == 1) {
            Message message = messages.get(0);

            if (message.direction != MessageDirection.Receive || message.content.getPersistFlag() != Persist_And_Count) {
                return;
            }

            Intent intent = new Intent(this, ConversationActivity.class);
            intent.putExtra("conversation", message.conversation);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
            if (message.content != null) {
                String pushContent = message.content.encode().pushContent;
                if (pushContent == null) {
                    if (message.content.getType() == MessageContentType.ContentType_Text) {
                        TextMessageContent textMessageContent = (TextMessageContent) message.content;
                        pushContent = textMessageContent.getContent();
                    } else if (message.content.getType() == MessageContentType.ContentType_Image) {
                        pushContent = "[图片]";
                    } else if (message.content.getType() == MessageContentType.ContentType_Voice) {
                        pushContent = "[语音]";
                    }
                }
                if (pushContent != null) {
                    int unreadCount = ChatManager.Instance().getUnreadCount(message.conversation).unread;
                    if (unreadCount > 1) {
                        pushContent = "[" + unreadCount + "条]" + pushContent;
                    }
                }
                if (pushContent != null) {
                    if (message.conversation.type == Single) {
                        UserInfo userInfo = ChatManager.Instance().getUserInfo(message.conversation.target, false);
                        builder.setContentTitle(userInfo == null ? "新消息" : userInfo.displayName);
                        builder.setContentText(pushContent);
                        notificationManager.notify(notificationId(message.conversation), builder.build());
                    } else if (message.conversation.type == Conversation.ConversationType.Group) {
                        GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(message.conversation.target, false);
                        builder.setContentTitle(groupInfo == null ? "群聊" : groupInfo.name);
                        builder.setContentText(pushContent);
                        notificationManager.notify(notificationId(message.conversation), builder.build());
                    }
                }
            }
        }
    }

    private int notificationId(Conversation conversation) {
        int id = conversation.type.getValue();
        id = id << 28;
        id = id | (conversation.line & 0x0fffffff);
        return id;
    }

}
