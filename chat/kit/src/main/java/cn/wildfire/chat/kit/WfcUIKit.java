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
import android.util.Log;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.kit.common.AppScopeViewModel;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.voip.AsyncPlayer;
import cn.wildfire.chat.kit.voip.MultiCallActivity;
import cn.wildfire.chat.kit.voip.SingleCallActivity;
import cn.wildfire.chat.kit.voip.VoipCallService;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.VideoProfile;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;


public class WfcUIKit implements AVEngineKit.AVEngineCallback, OnReceiveMessageListener, OnRecallMessageListener {

    private boolean isBackground = true;
    private Application application;
    private static ViewModelProvider viewModelProvider;
    private ViewModelStore viewModelStore;
    private AppServiceProvider appServiceProvider;
    private static WfcUIKit wfcUIKit;
    private boolean isSupportMoment = false;

    private WfcUIKit() {
    }

    public static WfcUIKit getWfcUIKit() {
        if (wfcUIKit == null) {
            wfcUIKit = new WfcUIKit();
        }
        return wfcUIKit;
    }

    public void init(Application application) {
        this.application = application;
        initWFClient(application);
        initMomentClient(application);
        //初始化表情控件
        LQREmotionKit.init(application, (context, path, imageView) -> Glide.with(context).load(path).apply(new RequestOptions().centerCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate()).into(imageView));

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onForeground() {
                WfcNotificationManager.getInstance().clearAllNotification(application);
                isBackground = false;

                // 处理没有后台弹出界面权限
                AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
                if (session != null) {
                    onReceiveCall(session);
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onBackground() {
                isBackground = true;
            }
        });

        viewModelStore = new ViewModelStore();
        ViewModelProvider.Factory factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application);
        viewModelProvider = new ViewModelProvider(viewModelStore, factory);
        OKHttpHelper.init(application.getApplicationContext());
    }

    public boolean isSupportMoment() {
        return isSupportMoment;
    }

    private void initWFClient(Application application) {
        ChatManager.init(application, Config.IM_SERVER_HOST);
        try {
            ChatManagerHolder.gChatManager = ChatManager.Instance();
            ChatManagerHolder.gChatManager.startLog();
            ChatManagerHolder.gChatManager.addOnReceiveMessageListener(this);
            ChatManagerHolder.gChatManager.addRecallMessageListener(this);

            ringPlayer = new AsyncPlayer(null);
            AVEngineKit.init(application, this);
            ChatManagerHolder.gAVEngine = AVEngineKit.Instance();
            ChatManagerHolder.gAVEngine.addIceServer(Config.ICE_ADDRESS, Config.ICE_USERNAME, Config.ICE_PASSWORD);
            ChatManagerHolder.gAVEngine.addIceServer(Config.ICE_ADDRESS2, Config.ICE_USERNAME, Config.ICE_PASSWORD);

            SharedPreferences sp = application.getSharedPreferences("config", Context.MODE_PRIVATE);
            String id = sp.getString("id", null);
            String token = sp.getString("token", null);
            if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(token)) {
                //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
                //另外不能多次connect，如果需要切换用户请先disconnect，然后3秒钟之后再connect（如果是用户手动登录可以不用等，因为用户操作很难3秒完成，如果程序自动切换请等3秒）
                ChatManagerHolder.gChatManager.connect(id, token);
            }
        } catch (NotInitializedExecption notInitializedExecption) {
            notInitializedExecption.printStackTrace();
        }
    }

    private void initMomentClient(Application application) {
        String momentClientClassName = "cn.wildfirechat.moment.MomentClient";
        try {
            Class clazz = Class.forName(momentClientClassName);
            Constructor constructor = clazz.getConstructor();
            Object o = constructor.newInstance();
            Method method = clazz.getMethod("init", Context.class);
            method.invoke(o, application);
            isSupportMoment = true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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
        List<String> participants = session.getParticipantIds();
        if (participants == null || participants.isEmpty()) {
            return;
        }

        boolean speakerOff = session.getConversation().type == Conversation.ConversationType.Single && session.isAudioOnly();
        AudioManager audioManager = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(speakerOff ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(!speakerOff);

        Conversation conversation = session.getConversation();
        if (conversation.type == Conversation.ConversationType.Single) {
            Intent intent = new Intent(WfcIntent.ACTION_VOIP_SINGLE);
            startActivity(application, intent);
        } else {
            Intent intent = new Intent(WfcIntent.ACTION_VOIP_MULTI);
            startActivity(application, intent);
        }
        VoipCallService.start(application, false);
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
    public static void singleCall(Context context, String targetId, boolean isAudioOnly) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(isAudioOnly ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(!isAudioOnly);

        Conversation conversation = new Conversation(Conversation.ConversationType.Single, targetId);
        AVEngineKit.Instance().startCall(conversation, Collections.singletonList(targetId), isAudioOnly, null);

        Intent voip = new Intent(context, SingleCallActivity.class);
        startActivity(context, voip);

        VoipCallService.start(context, false);
    }

    public static void multiCall(Context context, String groupId, List<String> participants, boolean isAudioOnly) {
        if (!AVEngineKit.isSupportMultiCall()) {
            Log.e("WfcKit", "avenginekit not support multi call");
            return;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);

        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupId);
        if (participants.size() >= 4) {
            AVEngineKit.Instance().setVideoProfile(VideoProfile.VP240P, false);
        } else if (participants.size() >= 6) {
            AVEngineKit.Instance().setVideoProfile(VideoProfile.VP120P, false);
        }
        AVEngineKit.Instance().startCall(conversation, participants, isAudioOnly, null);
        Intent intent = new Intent(context, MultiCallActivity.class);
        startActivity(context, intent);
    }

    private static void startActivity(Context context, Intent intent) {
        if (context instanceof Activity) {
            context.startActivity(intent);
        } else {
            Intent main = new Intent(WfcIntent.ACTION_MAIN);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivities(context, 100, new Intent[]{main, intent}, PendingIntent.FLAG_UPDATE_CURRENT);
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

    public AppServiceProvider getAppServiceProvider() {
        return this.appServiceProvider;
    }


    public void setAppServiceProvider(AppServiceProvider appServiceProvider) {
        this.appServiceProvider = appServiceProvider;
    }
}
