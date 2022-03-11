/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
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

import cn.wildfire.chat.kit.common.AppScopeViewModel;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.voip.AsyncPlayer;
import cn.wildfire.chat.kit.voip.MultiCallActivity;
import cn.wildfire.chat.kit.voip.SingleCallActivity;
import cn.wildfire.chat.kit.voip.VoipCallService;
import cn.wildfire.chat.kit.voip.conference.message.ConferenceChangeModeContent;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.VideoProfile;
import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.message.notification.PCLoginRequestMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnDeleteMessageListener;
import cn.wildfirechat.remote.OnFriendUpdateListener;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;


public class WfcUIKit implements AVEngineKit.AVEngineCallback, OnReceiveMessageListener, OnRecallMessageListener, OnDeleteMessageListener, OnFriendUpdateListener {

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
        UIUtils.application = application;
        initWFClient(application);
        initMomentClient(application);
        initPttClient(application);
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

        Log.d("WfcUIKit", "init end");
    }

    public boolean isSupportMoment() {
        return isSupportMoment;
    }

    public Application getApplication() {
        return application;
    }

    private void initWFClient(Application application) {
        ChatManager.init(application, Config.IM_SERVER_HOST);
        try {
            ChatManagerHolder.gChatManager = ChatManager.Instance();
            ChatManagerHolder.gChatManager.startLog();
            ChatManagerHolder.gChatManager.addOnReceiveMessageListener(this);
            ChatManagerHolder.gChatManager.addRecallMessageListener(this);
            ChatManagerHolder.gChatManager.addFriendUpdateListener(this);

            //当PC/Web在线时手机端是否静音，默认静音。如果修改为不默认静音，需要打开下面函数。
            //另外需要IM服务配置server.mobile_default_silent_when_pc_online为false。必须保持与服务器同步。
            //ChatManagerHolder.gChatManager.setDefaultSilentWhenPcOnline(false);

            ringPlayer = new AsyncPlayer(null);

            // 仅高级版支持，是否禁用双流模式
            //AVEngineKit.DISABLE_DUAL_STREAM_MODE = true;
            // 多人版，最多支持4人；高级版，最多支持9人
            //AVEngineKit.MAX_VIDEO_PARTICIPANT_COUNT = 9;
            // 多人版，最多支持9人；高级版，最多支持16人
            //AVEngineKit.MAX_AUDIO_PARTICIPANT_COUNT= 16;
            AVEngineKit.init(application, this);
            AVEngineKit.Instance().setVideoProfile(VideoProfile.VP360P, false);

            ChatManager.Instance().registerMessageContent(ConferenceChangeModeContent.class);
            ChatManagerHolder.gAVEngine = AVEngineKit.Instance();
            for (String[] server : Config.ICE_SERVERS) {
                ChatManagerHolder.gAVEngine.addIceServer(server[0], server[1], server[2]);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPttClient(Application application) {
        // 对讲机
        SharedPreferences sp = application.getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        boolean pttEnabled = sp.getBoolean("pttEnabled", true);
        if (pttEnabled){
            PTTClient.getInstance().init(application);
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
        ChatManager.Instance().getMainHandler().postDelayed(() -> {
            AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
            if (callSession == null || callSession.getState() != AVEngineKit.CallState.Incoming) {
                return;
            }

            List<String> participants = session.getParticipantIds();
            if (participants == null || participants.isEmpty()) {
                return;
            }

            Conversation conversation = session.getConversation();
            if (conversation.type == Conversation.ConversationType.Single) {
                Intent intent = new Intent(WfcIntent.ACTION_VOIP_SINGLE);
                startActivity(application, intent);
            } else {
                Intent intent = new Intent(WfcIntent.ACTION_VOIP_MULTI);
                startActivity(application, intent);
            }
            VoipCallService.start(application, false);
        }, 200);
    }

    private AsyncPlayer ringPlayer;

    @Override
    public void shouldStartRing(boolean isIncoming) {
        if (isIncoming && ChatManager.Instance().isVoipSilent()) {
            Log.d("wfcUIKit", "用户设置禁止voip通知，忽略来电提醒");
            return;
        }
        ChatManager.Instance().getMainHandler().postDelayed(() -> {
            AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
            if (callSession == null || (callSession.getState() != AVEngineKit.CallState.Incoming && callSession.getState() != AVEngineKit.CallState.Outgoing)) {
                return;
            }

            if (isIncoming) {
                Uri uri = Uri.parse("android.resource://" + application.getPackageName() + "/" + R.raw.incoming_call_ring);
                ringPlayer.play(application, uri, true, AudioManager.STREAM_RING);
            } else {
                Uri uri = Uri.parse("android.resource://" + application.getPackageName() + "/" + R.raw.outgoing_call_ring);
                ringPlayer.play(application, uri, true, AudioManager.STREAM_RING);
            }
        }, 200);
    }

    @Override
    public void shouldSopRing() {
        Log.d("wfcUIKit", "showStopRing");
        ringPlayer.stop();
    }

    // pls refer to https://stackoverflow.com/questions/11124119/android-starting-new-activity-from-application-class
    public static void singleCall(Context context, String targetId, boolean isAudioOnly) {
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, targetId);
        AVEngineKit.Instance().startCall(conversation, Collections.singletonList(targetId), isAudioOnly, null);

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(isAudioOnly ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(!isAudioOnly);

        Intent voip = new Intent(context, SingleCallActivity.class);
        startActivity(context, voip);

        VoipCallService.start(context, false);
    }

    public static void multiCall(Context context, String groupId, List<String> participants, boolean isAudioOnly) {
        if (!AVEngineKit.isSupportMultiCall()) {
            Log.e("WfcKit", "avenginekit not support multi call");
            return;
        }

        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupId);
        AVEngineKit.Instance().startCall(conversation, participants, isAudioOnly, null);

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);

        Intent intent = new Intent(context, MultiCallActivity.class);
        startActivity(context, intent);
    }

    public static void startActivity(Context context, Intent intent) {
        if (context instanceof Activity) {
            context.startActivity(intent);
            ((Activity) context).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            Intent main = new Intent(context.getPackageName() + ".main");
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivities(context, 100, new Intent[]{main, intent}, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            }else {
                pendingIntent = PendingIntent.getActivities(context, 100, new Intent[]{main, intent}, PendingIntent.FLAG_UPDATE_CURRENT);
            }
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
        long now = System.currentTimeMillis();
        long delta = ChatManager.Instance().getServerDeltaTime();
        if (messages != null) {
            for (Message msg : messages) {
                if (msg.content instanceof PCLoginRequestMessageContent && (now - (msg.serverTime - delta)) < 60 * 1000) {
                    PCLoginRequestMessageContent content = ((PCLoginRequestMessageContent) msg.content);
                    appServiceProvider.showPCLoginActivity(ChatManager.Instance().getUserId(), content.getSessionId(), content.getPlatform());
                    break;
                }
            }
        }

        if (isBackground && messages != null) {
            List<Message> msgs = new ArrayList<>(messages);
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

    @Override
    public void onDeleteMessage(Message message) {
        if (isBackground) {
            WfcNotificationManager.getInstance().handleDeleteMessage(application, message);
        }
    }

    @Override
    public void onFriendListUpdate(List<String> updateFriendList) {
        // do nothing

    }

    @Override
    public void onFriendRequestUpdate(List<String> newRequests) {
        if (isBackground) {
            if (newRequests == null || newRequests.isEmpty()) {
                return;
            }
            WfcNotificationManager.getInstance().handleFriendRequest(application, newRequests);
        }
    }

    public AppServiceProvider getAppServiceProvider() {
        return this.appServiceProvider;
    }


    public void setAppServiceProvider(AppServiceProvider appServiceProvider) {
        this.appServiceProvider = appServiceProvider;
    }
}
