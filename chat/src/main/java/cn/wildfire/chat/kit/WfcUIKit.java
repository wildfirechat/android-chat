package cn.wildfire.chat.kit;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.lqr.emoji.LQREmotionKit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.kit.common.AppScopeViewModel;
import cn.wildfire.chat.kit.voip.AsyncPlayer;
import cn.wildfire.chat.kit.voip.SingleVoipCallActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.BuildConfig;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.push.PushService;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;


public class WfcUIKit implements AVEngineKit.AVEngineCallback, OnReceiveMessageListener, OnRecallMessageListener {

    private boolean isBackground = true;
    private static Application application;
    private static ViewModelProvider viewModelProvider;
    private ViewModelStore viewModelStore;

    public void init(Application application) {
        WfcUIKit.application = application;
        initWFClient(application);
        //初始化表情控件
        LQREmotionKit.init(application, (context, path, imageView) -> Glide.with(context).load(path).apply(new RequestOptions().centerCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate()).into(imageView));

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onForeground() {
                PushService.clearNotification(application);
                WfcNotificationManager.getInstance().clearAllNotification(application);
                isBackground = false;
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onBackground() {
                isBackground = true;
                viewModelStore.clear();
            }
        });

        viewModelStore = new ViewModelStore();
        ViewModelProvider.Factory factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application);
        viewModelProvider = new ViewModelProvider(viewModelStore, factory);
    }

    private void initWFClient(Application application) {
        ChatManager.init(application, Config.IM_SERVER_HOST, Config.IM_SERVER_PORT);
        try {
            ChatManagerHolder.gChatManager = ChatManager.Instance();
            ChatManagerHolder.gChatManager.startLog();
            ChatManagerHolder.gChatManager.addOnReceiveMessageListener(this);
            ChatManagerHolder.gChatManager.addRecallMessageListener(this);
            PushService.init(application, BuildConfig.APPLICATION_ID);

            ringPlayer = new AsyncPlayer(null);
            AVEngineKit.init(application, this);
            ChatManagerHolder.gAVEngine = AVEngineKit.Instance();
            ChatManagerHolder.gAVEngine.addIceServer(Config.ICE_ADDRESS, Config.ICE_USERNAME, Config.ICE_PASSWORD);

            SharedPreferences sp = application.getSharedPreferences("config", Context.MODE_PRIVATE);
            String id = sp.getString("id", null);
            String token = sp.getString("token", null);
            if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(token)) {
                ChatManagerHolder.gChatManager.connect(id, token);
            }
        } catch (NotInitializedExecption notInitializedExecption) {
            notInitializedExecption.printStackTrace();
        }
    }

    /**
     * 当{@link androidx.lifecycle.ViewModel} 需要跨{@link android.app.Activity} 共享数据时使用
     */
    public static <T extends ViewModel> T getAppScopeViewModel(@NonNull Class<T> modelClass) {
        if (!AppScopeViewModel.class.isAssignableFrom(modelClass)) {
            throw new IllegalArgumentException("the model class should be subclass of AppScopeViewModel");
        }
        return viewModelProvider.get(modelClass);
    }

    @Override
    public void onReceiveCall(AVEngineKit.CallSession session) {
        onCall(application, session.getClientId(), false, session.isAudioOnly());
    }

    private AsyncPlayer ringPlayer;

    @Override
    public void shouldStartRing(boolean isIncomming) {
        if (isIncomming) {
            Uri uri = Uri.parse("android.resource://" + application.getPackageName() + "/" + R.raw.incoming_call_ring);
            ringPlayer.play(application, uri, true, AudioManager.STREAM_RING);
        } else {
            Uri uri = Uri.parse("android.resource://" + application.getPackageName() + "/" + R.raw.outgoing_call_ring);
            ringPlayer.play(application, uri, true, AudioManager.STREAM_RING);
        }
    }

    @Override
    public void shouldSopRing() {
        ringPlayer.stop();
    }

    // pls refer to https://stackoverflow.com/questions/11124119/android-starting-new-activity-from-application-class
    public static void onCall(Context context, String targetId, boolean isMo, boolean isAudioOnly) {
        Intent voip = new Intent(WfcIntent.ACTION_VOIP_SINGLE);
        voip.putExtra(SingleVoipCallActivity.EXTRA_MO, isMo);
        voip.putExtra(SingleVoipCallActivity.EXTRA_TARGET, targetId);
        voip.putExtra(SingleVoipCallActivity.EXTRA_AUDIO_ONLY, isAudioOnly);

        if (context instanceof Activity) {
            context.startActivity(voip);
        } else {
            Intent main = new Intent(WfcIntent.ACTION_MAIN);
            voip.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivities(context, 100, new Intent[]{main, voip},  PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        if (isBackground) {
            // FIXME: 2018/5/28 只是临时方案，No_Persist消息，我觉得不应当到这儿，注册监听时，
            // 就表明自己关系哪些类型的消息, 设置哪些种类的消息

            if (messages == null) {
                return;
            }

            List<Message> msgs = new ArrayList<>(messages);
            long now = System.currentTimeMillis();
            long delta = ChatManager.Instance().getServerDeltaTime();
            Iterator<Message> iterator = msgs.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
                if (message.content.getPersistFlag() == PersistFlag.No_Persist
                        || now - (message.serverTime - delta) > 10 * 1000) {
                    iterator.remove();
                }
            }
            WfcNotificationManager.getInstance().handleReceiveMessage(application, msgs);
        } else {
            // do nothing
        }
    }

    @Override
    public void onRecallMessage(Message message) {
        if (isBackground) {
            WfcNotificationManager.getInstance().handleRecallMessage(application, message);
        }
    }
}
