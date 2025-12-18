/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;


import static android.content.Context.BIND_AUTO_CREATE;
import static cn.wildfirechat.model.Conversation.ConversationType.Channel;
import static cn.wildfirechat.model.Conversation.ConversationType.Group;
import static cn.wildfirechat.model.Conversation.ConversationType.SecretChat;
import static cn.wildfirechat.model.Conversation.ConversationType.Single;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import cn.wildfirechat.ErrorCode;
import cn.wildfirechat.UserSource;
import cn.wildfirechat.ashmen.AshmenWrapper;
import cn.wildfirechat.client.ClientService;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.client.ICreateChannelCallback;
import cn.wildfirechat.client.ICreateSecretChatCallback;
import cn.wildfirechat.client.IGeneralCallback;
import cn.wildfirechat.client.IGeneralCallback2;
import cn.wildfirechat.client.IGeneralCallbackInt;
import cn.wildfirechat.client.IGetAuthorizedMediaUrlCallback;
import cn.wildfirechat.client.IGetConversationListCallback;
import cn.wildfirechat.client.IGetFileRecordCallback;
import cn.wildfirechat.client.IGetGroupCallback;
import cn.wildfirechat.client.IGetGroupMemberCallback;
import cn.wildfirechat.client.IGetMessageCallback;
import cn.wildfirechat.client.IGetRemoteDomainInfosCallback;
import cn.wildfirechat.client.IGetRemoteMessagesCallback;
import cn.wildfirechat.client.IGetUploadUrlCallback;
import cn.wildfirechat.client.IGetUserCallback;
import cn.wildfirechat.client.IGetUserInfoListCallback;
import cn.wildfirechat.client.IOnChannelInfoUpdateListener;
import cn.wildfirechat.client.IOnConferenceEventListener;
import cn.wildfirechat.client.IOnConnectToServerListener;
import cn.wildfirechat.client.IOnConnectionStatusChangeListener;
import cn.wildfirechat.client.IOnDomainInfoUpdateListener;
import cn.wildfirechat.client.IOnFriendUpdateListener;
import cn.wildfirechat.client.IOnGroupInfoUpdateListener;
import cn.wildfirechat.client.IOnGroupMembersUpdateListener;
import cn.wildfirechat.client.IOnReceiveMessageListener;
import cn.wildfirechat.client.IOnSecretChatStateListener;
import cn.wildfirechat.client.IOnSecretMessageBurnStateListener;
import cn.wildfirechat.client.IOnSettingUpdateListener;
import cn.wildfirechat.client.IOnTrafficDataListener;
import cn.wildfirechat.client.IOnUserInfoUpdateListener;
import cn.wildfirechat.client.IOnUserOnlineEventListener;
import cn.wildfirechat.client.IRemoteClient;
import cn.wildfirechat.client.IUploadMediaCallback;
import cn.wildfirechat.client.IWatchUserOnlineStateCallback;
import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.client.Platform;
import cn.wildfirechat.message.ArticlesMessageContent;
import cn.wildfirechat.message.CallStartMessageContent;
import cn.wildfirechat.message.CardMessageContent;
import cn.wildfirechat.message.ChannelMenuEventMessageContent;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.ConferenceInviteMessageContent;
import cn.wildfirechat.message.EnterChannelChatMessageContent;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.JoinCallRequestMessageContent;
import cn.wildfirechat.message.LeaveChannelChatMessageContent;
import cn.wildfirechat.message.LinkMessageContent;
import cn.wildfirechat.message.LocationMessageContent;
import cn.wildfirechat.message.MarkUnreadMessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.message.MultiCallOngoingMessageContent;
import cn.wildfirechat.message.NotDeliveredMessageContent;
import cn.wildfirechat.message.PTTSoundMessageContent;
import cn.wildfirechat.message.PTextMessageContent;
import cn.wildfirechat.message.RawMessageContent;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.StickerMessageContent;
import cn.wildfirechat.message.StreamingTextGeneratedMessageContent;
import cn.wildfirechat.message.StreamingTextGeneratingMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.message.UnknownMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.message.notification.AddGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.ChangeGroupNameNotificationContent;
import cn.wildfirechat.message.notification.ChangeGroupPortraitNotificationContent;
import cn.wildfirechat.message.notification.CreateGroupNotificationContent;
import cn.wildfirechat.message.notification.DeleteMessageContent;
import cn.wildfirechat.message.notification.DismissGroupNotificationContent;
import cn.wildfirechat.message.notification.FriendAddedMessageContent;
import cn.wildfirechat.message.notification.FriendGreetingMessageContent;
import cn.wildfirechat.message.notification.GroupAllowMemberNotificationContent;
import cn.wildfirechat.message.notification.GroupJoinTypeNotificationContent;
import cn.wildfirechat.message.notification.GroupMuteMemberNotificationContent;
import cn.wildfirechat.message.notification.GroupMuteNotificationContent;
import cn.wildfirechat.message.notification.GroupPrivateChatNotificationContent;
import cn.wildfirechat.message.notification.GroupRejectJoinNotificationContent;
import cn.wildfirechat.message.notification.GroupSetManagerNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberVisibleNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupAliasNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupExtraNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupMemberExtraNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupSettingsNotificationContent;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.message.notification.PCLoginRequestMessageContent;
import cn.wildfirechat.message.notification.QuitGroupNotificationContent;
import cn.wildfirechat.message.notification.QuitGroupVisibleNotificationContent;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.message.notification.RichNotificationMessageContent;
import cn.wildfirechat.message.notification.StartSecretChatMessageContent;
import cn.wildfirechat.message.notification.TipNotificationContent;
import cn.wildfirechat.message.notification.TransferGroupOwnerNotificationContent;
import cn.wildfirechat.model.BurnMessageInfo;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.ChatRoomInfo;
import cn.wildfirechat.model.ChatRoomMembersInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.ConversationSearchResult;
import cn.wildfirechat.model.DomainInfo;
import cn.wildfirechat.model.FileRecord;
import cn.wildfirechat.model.FileRecordOrder;
import cn.wildfirechat.model.Friend;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.model.ModifyChannelInfoType;
import cn.wildfirechat.model.ModifyGroupInfoType;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.NullChannelInfo;
import cn.wildfirechat.model.NullConversationInfo;
import cn.wildfirechat.model.NullDomainInfo;
import cn.wildfirechat.model.NullGroupInfo;
import cn.wildfirechat.model.NullUserInfo;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.model.QuoteInfo;
import cn.wildfirechat.model.ReadEntry;
import cn.wildfirechat.model.SecretChatInfo;
import cn.wildfirechat.model.Socks5ProxyInfo;
import cn.wildfirechat.model.UnreadCount;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.model.UserOnlineState;
import cn.wildfirechat.utils.WfcUtils;

/**
 * Created by WF Chat on 2017/12/11.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ChatManager {
    private static final String TAG = ChatManager.class.getName();

    private String SERVER_HOST;

    private static IRemoteClient mClient;

    private static ChatManager INST;
    private static Context gContext;

    private String userId;
    private String token;
    private Handler mainHandler;
    private Handler internalWorkHandler;
    private Handler workHandler;
    private String deviceToken;
    private String clientId;
    private int pushType;
    private final Map<Integer, Class<? extends MessageContent>> messageContentMap = new ConcurrentHashMap<>();
    private final Map<Integer, PersistFlag> messagePersistFlagMap = new ConcurrentHashMap<>();
    private boolean isLiteMode = false;
    private boolean isLowBPSMode = false;
    private UserSource userSource;

    private boolean startLog;
    private String sendLogCommand;
    private int connectionStatus;
    private int receiptStatus = -1; // 1, enable
    private int groupReceiptStatus = -1; // 1, enable
    private int userReceiptStatus = -1; //1, enable

    private int backupAddressStrategy = 0;
    private String backupAddressHost = null;
    private int backupAddressPort = 80;
    private String protoUserAgent = null;
    private final Map<String, String> protoHttpHeaderMap = new ConcurrentHashMap<>();

    private boolean useSM4 = false;
    private boolean isPad = false;
    private boolean useAES256 = false;
    private boolean tcpShortLink = false;
    private int timeOffset = 0;
    private boolean rawMsg = false;
    private boolean noUseFts = false;
    private boolean checkSignature = false;
    private boolean defaultSilentWhenPCOnline = true;

    //心跳间隔，单位为秒。
    private int heartBeatInterval = -1;

    private Socks5ProxyInfo proxyInfo;

    private boolean isBackground = true;
    private List<OnReceiveMessageListener> onReceiveMessageListeners = new ArrayList<>();
    private List<OnConnectionStatusChangeListener> onConnectionStatusChangeListeners = new ArrayList<>();
    private List<OnTrafficDataListener> onTrafficDataListeners = new ArrayList<>();
    private List<OnConnectToServerListener> onConnectToServerListeners = new ArrayList<>();
    private List<OnSendMessageListener> sendMessageListeners = new ArrayList<>();
    private List<OnGroupInfoUpdateListener> groupInfoUpdateListeners = new ArrayList<>();
    private List<OnGroupMembersUpdateListener> groupMembersUpdateListeners = new ArrayList<>();
    private List<OnUserInfoUpdateListener> userInfoUpdateListeners = new ArrayList<>();
    private List<OnSettingUpdateListener> settingUpdateListeners = new ArrayList<>();
    private List<OnFriendUpdateListener> friendUpdateListeners = new ArrayList<>();
    private List<OnConversationInfoUpdateListener> conversationInfoUpdateListeners = new ArrayList<>();
    private List<OnRecallMessageListener> recallMessageListeners = new ArrayList<>();
    private List<OnDeleteMessageListener> deleteMessageListeners = new ArrayList<>();
    private List<OnChannelInfoUpdateListener> channelInfoUpdateListeners = new ArrayList<>();
    private List<OnMessageUpdateListener> messageUpdateListeners = new ArrayList<>();
    private List<OnClearMessageListener> clearMessageListeners = new ArrayList<>();
    private List<OnRemoveConversationListener> removeConversationListeners = new ArrayList<>();

    private List<IMServiceStatusListener> imServiceStatusListeners = new ArrayList<>();
    private List<OnMessageDeliverListener> messageDeliverListeners = new ArrayList<>();
    private List<OnMessageReadListener> messageReadListeners = new ArrayList<>();
    private List<OnConferenceEventListener> conferenceEventListeners = new ArrayList<>();
    private List<OnUserOnlineEventListener> userOnlineEventListeners = new ArrayList<>();
    private List<SecretChatStateChangeListener> secretChatStateChangeListeners = new ArrayList<>();
    private List<SecretMessageBurnStateListener> secretMessageBurnStateListeners = new ArrayList<>();
    private List<OnDomainInfoUpdateListener> domainInfoUpdateListeners = new ArrayList<>();


    // key = userId
    private LruCache<String, UserInfo> userInfoCache;
    // key = memberId@groupId
    private LruCache<String, GroupMember> groupMemberCache;

    private Map<String, UserOnlineState> userOnlineStateMap;

    private Class<? extends DefaultPortraitProvider> defaultPortraitProviderClazz;
    private Class<? extends UrlRedirector> urlRedirectorClazz;
    private UrlRedirector urlRedirector;

    private boolean isClientServiceInMainProcess = false;

    public enum SearchUserType {
        //模糊搜索displayName，精确搜索name或电话号码
        General(0),

        //精确搜索name或电话号码
        NameOrMobile(1),

        //精确搜索name
        Name(2),

        //精确搜索电话号码
        Mobile(3),

        //精确搜索用户Id
        UserId(4),

        //精确搜索name或电话号码或用户ID
        NameOrMobileOrUserId(5);

        SearchUserType(int value) {
        }

        public static SearchUserType type(int type) {
            SearchUserType searchUserType = null;
            switch (type) {
                case 0:
                    searchUserType = General;
                    break;
                case 1:
                    searchUserType = NameOrMobile;
                    break;
                case 2:
                    searchUserType = Name;
                    break;
                case 3:
                    searchUserType = Mobile;
                    break;
                case 4:
                    searchUserType = UserId;
                    break;
                case 5:
                    searchUserType = NameOrMobileOrUserId;
                    break;
                default:
                    throw new IllegalArgumentException("type " + searchUserType + " is invalid");
            }
            return searchUserType;
        }
    }

    public enum UserSearchUserType {
        //搜索所有，包括普通用户和机器人。
        All(0),

        //只搜索普通用户
        OnlyUser(1),

        //只搜索机器人
        OnlyRobot(2);

        UserSearchUserType(int value) {
        }

        public static UserSearchUserType type(int type) {
            UserSearchUserType userSearchUserType = null;
            switch (type) {
                case 0:
                    userSearchUserType = All;
                    break;
                case 1:
                    userSearchUserType = OnlyUser;
                    break;
                case 2:
                    userSearchUserType = OnlyRobot;
                    break;
                default:
                    throw new IllegalArgumentException("type " + userSearchUserType + " is invalid");
            }
            return userSearchUserType;
        }
    }

    /**
     * 禁止搜索当前用户的掩码。
     * <p>
     * - DisplayName: 第1位是否禁止搜索昵称
     * - Name: 第2位是否禁止搜索账户
     * - Mobile: 第3位是否禁止搜索电话号码
     * - UserId: 第4位是否禁止搜索用户ID
     */
    public interface DisableSearchUserMask {
        int DisplayName = 1;
        int Name = 2;
        int Mobile = 4;
        int UserId = 8;
    }

    public enum SecretChatState {
        //密聊会话建立中
        Starting(0),

        //密聊会话接受中
        Accepting(1),

        //密聊会话已建立
        Established(2),

        //密聊会话已取消
        Canceled(3);

        private final int value;

        SecretChatState(int value) {
            this.value = value;
        }

        public static SecretChatState fromValue(int value) {
            for (SecretChatState secretChatState : SecretChatState.values()) {
                if (secretChatState.value == value)
                    return secretChatState;
            }
            return Canceled;
        }
    }

    /**
     * 获取当前用户的id
     *
     * @return 用户id
     */
    public String getUserId() {
        return userId;
    }

    public interface IGeneralCallback3 {
        void onSuccess(List<String> results);

        void onFailure(int errorCode);
    }

    private ChatManager(String serverHost) {
        this.SERVER_HOST = serverHost;
    }

    public static ChatManager Instance() throws NotInitializedExecption {
        if (INST == null) {
            throw new NotInitializedExecption();
        }
        return INST;
    }

    /**
     * 初始化，只能在主进程调用，否则会导致重复收到消息
     * serverHost可以是IP，可以是域名
     *
     * @param context
     * @param imServerHost im server的域名或ip
     * @return
     */
    public static void init(Application context, String imServerHost) {
        init(context.getApplicationContext(), imServerHost);
    }

    /**
     * 初始化，只能在主进程调用，否则会导致重复收到消息
     * serverHost可以是IP，可以是域名，如果是域名的话只支持主域名或www域名，二级域名不支持！
     * 例如：example.com或www.example.com是支持的；xx.example.com或xx.yy.example.com是不支持的。
     *
     * @param context
     * @param imServerHost im server的域名或ip
     * @return
     */
    public static void init(Context context, String imServerHost) {
        Log.d(TAG, "init " + imServerHost);
        if (imServerHost != null) {
            checkSDKHost(imServerHost);
        }
        if (INST != null) {
            // TODO: Already initialized
            return;
        }
//        if (TextUtils.isEmpty(imServerHost)) {
//            throw new IllegalArgumentException("imServerHost must be empty");
//        }
        gContext = context;
        INST = new ChatManager(imServerHost);
        INST.mainHandler = new Handler();
        INST.userInfoCache = new LruCache<>(1024);
        INST.groupMemberCache = new LruCache<>(1024);
        INST.userOnlineStateMap = new ConcurrentHashMap<>();
        HandlerThread internalHandlerthread = new HandlerThread("internalWorkHandler");
        internalHandlerthread.start();
        HandlerThread workHandlerThread = new HandlerThread("workHandler");
        workHandlerThread.start();
        INST.internalWorkHandler = new Handler(internalHandlerthread.getLooper());
        INST.workHandler = new Handler(workHandlerThread.getLooper());
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onForeground() {
                INST.isBackground = false;
                if (mClient == null) {
                    return;
                }
                try {
                    mClient.setForeground(1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onBackground() {
                INST.isBackground = true;
                if (mClient == null) {
                    return;
                }
                try {
                    mClient.setForeground(0);
                    INST.reportBadgeNumber();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        INST.checkRemoteService();

        INST.cleanLogFiles();
        INST.registerCoreMessageContents();
    }

    public Context getApplicationContext() {
        return gContext;
    }

    /**
     * 当有自己的用户账号体系，不想使用野火IM提供的用户信息托管服务时，调用此方法设置用户信息源
     *
     * @param userSource 用户信息源
     */
    public void setUserSource(UserSource userSource) {
        this.userSource = userSource;
    }

    /**
     * 获取当前的连接状态
     *
     * @return 连接状态，参考{@link ConnectionStatus}
     */
    public int getConnectionStatus() {
        return connectionStatus;
    }

    /**
     * App在后台时，如果需要强制连上服务器并收消息，调用此方法。后台时无法保证长时间连接中。
     */
    public void forceConnect() {
        if (mClient != null) {
            try {
                mClient.setForeground(1);
                if (INST.isBackground) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mClient != null) {
                                try {
                                    mClient.setForeground(INST.isBackground ? 1 : 0);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }, 3000);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 连接状态回调
     *
     * @param status 连接状态
     */
    private void onConnectionStatusChange(final int status) {
        Log.d(TAG, "connectionStatusChange " + status);
        if (status == ConnectionStatus.ConnectionStatusTokenIncorrect || status == ConnectionStatus.ConnectionStatusSecretKeyMismatch) {
            // TODO
            Log.d(TAG, "连接失败，请参考：" + "https://docs.wildfirechat.cn/faq/general.html");
        }

        //连接成功，如果Manager有缓存需要清掉
        if (status == ConnectionStatus.ConnectionStatusConnected) {
            receiptStatus = -1;
            userReceiptStatus = -1;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                connectionStatus = status;
                Iterator<OnConnectionStatusChangeListener> iterator = onConnectionStatusChangeListeners.iterator();
                OnConnectionStatusChangeListener listener;
                while (iterator.hasNext()) {
                    listener = iterator.next();
                    listener.onConnectionStatusChange(status);
                }
            }
        });
    }

    /**
     * 连接状态回调
     *
     * @param host 服务器host
     * @param ip   服务器ip
     * @param port 服务器port
     */
    private void onConnectToServer(final String host, final String ip, final int port) {
        Log.e(TAG, "connectToServer " + host + " " + ip + " " + port + " " + getConnectionStatus());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator<OnConnectToServerListener> iterator = onConnectToServerListeners.iterator();
                OnConnectToServerListener listener;
                while (iterator.hasNext()) {
                    listener = iterator.next();
                    listener.onConnectToServer(host, ip, port);
                }
            }
        });
    }

    /**
     * 消息被撤回
     *
     * @param messageUid
     */
    private void onRecallMessage(final long messageUid) {
        Message message = getMessageByUid(messageUid);
        // 想撤回的消息已经被删除
        if (message == null) {
            message = new Message();
            // 一般是聊天室
            message.messageUid = messageUid;
        }
        Message finalMessage = message;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (OnRecallMessageListener listener : recallMessageListeners) {
                    listener.onRecallMessage(finalMessage);
                }
            }
        });
    }

    /**
     * 消息被通过server api 删除
     *
     * @param messageUid
     */
    private void onDeleteMessage(final long messageUid) {
        Message message = new Message();
        message.messageUid = messageUid;
        mainHandler.post(() -> {
            for (OnDeleteMessageListener listener : deleteMessageListeners) {
                listener.onDeleteMessage(message);
            }
        });
    }

    /**
     * 收到新消息
     *
     * @param messages
     * @param hasMore  是否还有更多消息待收取
     */
    private void onReceiveMessage(final List<Message> messages, final boolean hasMore) {
        mainHandler.post(() -> {
            Iterator<OnReceiveMessageListener> iterator = onReceiveMessageListeners.iterator();
            OnReceiveMessageListener listener;
            while (iterator.hasNext()) {
                listener = iterator.next();
                listener.onReceiveMessage(messages, hasMore);
            }

            // 消息数大于时，认为是历史消息同步，不通知群被删除
            if (messages.size() > 10) {
                return;
            }
            for (Message message : messages) {
                if ((message.content instanceof QuitGroupNotificationContent && ((QuitGroupNotificationContent) message.content).operator.equals(getUserId()))
                    || (message.content instanceof KickoffGroupMemberNotificationContent && ((KickoffGroupMemberNotificationContent) message.content).kickedMembers.contains(getUserId()))
                    || message.content instanceof DismissGroupNotificationContent) {
                    for (OnRemoveConversationListener l : removeConversationListeners) {
                        l.onConversationRemove(message.conversation);
                    }
                }
            }
        });
    }

    private void onMsgDelivered(Map<String, Long> deliveries) {
        mainHandler.post(() -> {
            if (messageDeliverListeners != null) {
                for (OnMessageDeliverListener listener : messageDeliverListeners) {
                    listener.onMessageDelivered(deliveries);
                }
            }
        });
    }

    private void onMsgReaded(List<ReadEntry> readEntries) {
        mainHandler.post(() -> {
            if (messageReadListeners != null) {
                for (OnMessageReadListener listener : messageReadListeners) {
                    listener.onMessageRead(readEntries);
                }
            }
        });
    }

    /**
     * 用户信息更新
     *
     * @param userInfos
     */
    private void onUserInfoUpdate(List<UserInfo> userInfos) {
        if (userInfos == null || userInfos.isEmpty()) {
            return;
        }
        for (UserInfo info : userInfos) {
            userInfoCache.put(info.uid, info);
        }
        mainHandler.post(() -> {
            for (OnUserInfoUpdateListener listener : userInfoUpdateListeners) {
                listener.onUserInfoUpdate(userInfos);
            }
        });
    }

    /**
     * 群信息更新
     *
     * @param groupInfos
     */
    private void onGroupInfoUpdated(List<GroupInfo> groupInfos) {
        if (groupInfos == null || groupInfos.isEmpty()) {
            return;
        }
        mainHandler.post(() -> {
            for (OnGroupInfoUpdateListener listener : groupInfoUpdateListeners) {
                listener.onGroupInfoUpdate(groupInfos);
            }

        });
    }

    /**
     * 群成员信息更新
     *
     * @param groupId
     * @param groupMembers
     */
    private void onGroupMembersUpdate(String groupId, List<GroupMember> groupMembers) {
        if (groupMembers == null || groupMembers.isEmpty()) {
            return;
        }
        for (GroupMember member : groupMembers) {
            groupMemberCache.remove(groupMemberCacheKey(groupId, member.memberId));
        }
        mainHandler.post(() -> {
            for (OnGroupMembersUpdateListener listener : groupMembersUpdateListeners) {
                listener.onGroupMembersUpdate(groupId, groupMembers);
            }
        });
    }

    private void onFriendListUpdated(List<String> friendList) {
        mainHandler.post(() -> {
            for (OnFriendUpdateListener listener : friendUpdateListeners) {
                listener.onFriendListUpdate(friendList);
            }
        });
        onUserInfoUpdate(getUserInfos(friendList, null));
    }

    private void onFriendReqeustUpdated(List<String> newRequests) {
        mainHandler.post(() -> {
            for (OnFriendUpdateListener listener : friendUpdateListeners) {
                listener.onFriendRequestUpdate(newRequests);
            }
        });
    }

    private void onSettingUpdated() {
        mainHandler.post(() -> {
            for (OnSettingUpdateListener listener : settingUpdateListeners) {
                listener.onSettingUpdate();
            }
        });
    }

    private void onChannelInfoUpdate(List<ChannelInfo> channelInfos) {
        mainHandler.post(() -> {
            for (OnChannelInfoUpdateListener listener : channelInfoUpdateListeners) {
                listener.onChannelInfoUpdate(channelInfos);
            }
        });
    }

    private void onConferenceEvent(String event) {
        mainHandler.post(() -> {
            for (OnConferenceEventListener listener : conferenceEventListeners) {
                listener.onConferenceEvent(event);
            }
        });
    }

    private void onUserOnlineEvent(UserOnlineState[] userOnlineStates) {
        mainHandler.post(() -> {
            for (UserOnlineState userOnlineState : userOnlineStates) {
                userOnlineStateMap.put(userOnlineState.getUserId(), userOnlineState);
            }

            for (OnUserOnlineEventListener listener : userOnlineEventListeners) {
                listener.onUserOnlineEvent(userOnlineStateMap);
            }
        });
    }

    private void onSecretChatStateChanged(String targetId, int state) {
        mainHandler.post(() -> {
            for (SecretChatStateChangeListener listener : secretChatStateChangeListeners) {
                listener.onSecretChatStateChanged(targetId, SecretChatState.fromValue(state));
            }
        });
    }

    private void onSecretMessageStartBurning(String targetId, long playedMsgId) {
        mainHandler.post(() -> {
            for (SecretMessageBurnStateListener listener : secretMessageBurnStateListeners) {
                listener.onSecretMessageStartBurning(targetId, playedMsgId);
            }
        });
    }

    private void onSecretMessageBurned(List<Long> messageIds) {
        mainHandler.post(() -> {
            for (SecretMessageBurnStateListener listener : secretMessageBurnStateListeners) {
                listener.onSecretMessageBurned(messageIds);
            }
        });
    }

    private void onDomainInfoUpdate(DomainInfo domainInfo) {
        mainHandler.post(() -> {
            for (OnDomainInfoUpdateListener listener : domainInfoUpdateListeners) {
                listener.onDomainInfoUpdate(domainInfo);
            }
        });
    }

    /**
     * 获取用户的在线状态。
     *
     * @param userId 用户ID
     * @return 用户的在线状态。
     * @discussion 专业版IM服务开启在线状态的情况下，好友会自动同步在线状态。非好友需要订阅才能得到用户在线状态。
     */
    public UserOnlineState getUserOnlineState(String userId) {
        return userOnlineStateMap.get(userId);
    }

    /**
     * 获取所有已知的在线状态。
     *
     * @return 所有已知的在线状态。
     */
    public Map<String, UserOnlineState> getUserOnlineStateMap() {
        return userOnlineStateMap;
    }

    private void onTrafficData(long send, long recv) {
        mainHandler.post(() -> {
            for (OnTrafficDataListener listener : onTrafficDataListeners) {
                listener.onTrafficData(send, recv);
            }
        });
    }

    /**
     * 订阅在线状态。
     *
     * @param conversationType 订阅类型，仅支持单聊和群聊。单聊会订阅对方的在线状态，群聊时会订阅群内所有成员的在线状态。
     * @param targets          订阅目标列表
     * @param duration         订阅时长，单位是秒，最大不能超过
     * @param callback         订阅回调
     */
    public void watchOnlineState(int conversationType, String[] targets, int duration, WatchOnlineStateCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }
        try {
            mClient.watchUserOnlineState(conversationType, targets, duration, new IWatchUserOnlineStateCallback.Stub() {
                @Override
                public void onSuccess(UserOnlineState[] states) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            for (UserOnlineState state : states) {
                                userOnlineStateMap.put(state.getUserId(), state);
                            }
                            callback.onSuccess(states);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消订阅在线状态。
     *
     * @param conversationType 取消订阅类型，仅支持单聊和群聊。单聊会订阅对方的在线状态，群聊时会订阅群内所有成员的在线状态。
     * @param targets          取消订阅目标列表
     * @param callback         订阅回调
     */
    public void unWatchOnlineState(int conversationType, String[] targets, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }
        try {
            mClient.unwatchOnlineState(conversationType, targets, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(callback::onSuccess);
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否开启在线状态功能。
     *
     * @return 是否开启在线状态功能
     */
    public boolean isEnableUserOnlineState() {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            return mClient.isEnableUserOnlineState();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 添加新消息监听, 记得调用{@link #removeOnReceiveMessageListener(OnReceiveMessageListener)}删除监听
     *
     * @param listener
     */
    public void addOnReceiveMessageListener(OnReceiveMessageListener listener) {
        if (listener == null) {
            return;
        }
        onReceiveMessageListeners.add((listener));
    }

    /**
     * 删除消息监听
     *
     * @param listener
     */
    public void removeOnReceiveMessageListener(OnReceiveMessageListener listener) {
        if (listener == null) {
            return;
        }
        onReceiveMessageListeners.remove(listener);
    }

    /**
     * 添加发送消息监听
     *
     * @param listener
     */
    public void addSendMessageListener(OnSendMessageListener listener) {
        if (listener == null) {
            return;
        }
        sendMessageListeners.add(listener);
    }

    /**
     * 删除发送消息监听
     *
     * @param listener
     */
    public void removeSendMessageListener(OnSendMessageListener listener) {
        sendMessageListeners.remove(listener);
    }

    /**
     * 添加连接状态监听
     *
     * @param listener
     */
    public void addConnectionChangeListener(OnConnectionStatusChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (!onConnectionStatusChangeListeners.contains(listener)) {
            onConnectionStatusChangeListeners.add(listener);
        }
    }

    /**
     * 删除连接状态监听
     *
     * @param listener
     */
    public void removeConnectionChangeListener(OnConnectionStatusChangeListener listener) {
        if (listener == null) {
            return;
        }
        onConnectionStatusChangeListeners.remove(listener);
    }

    /**
     * 添加连接到服务监听
     *
     * @param listener
     */
    public void addConnectToServerListener(OnConnectToServerListener listener) {
        if (listener == null) {
            return;
        }
        if (!onConnectToServerListeners.contains(listener)) {
            onConnectToServerListeners.add(listener);
        }
    }

    /**
     * 删除连接到服务监听
     *
     * @param listener
     */
    public void removeConnectToServerListener(OnConnectToServerListener listener) {
        if (listener == null) {
            return;
        }
        onConnectToServerListeners.remove(listener);
    }

    /**
     * 添加群信息更新监听
     *
     * @param listener
     */
    public void addGroupInfoUpdateListener(OnGroupInfoUpdateListener listener) {
        if (listener == null) {
            return;
        }
        groupInfoUpdateListeners.add(listener);
    }

    /**
     * 删除群信息监听
     *
     * @param listener
     */
    public void removeGroupInfoUpdateListener(OnGroupInfoUpdateListener listener) {
        groupInfoUpdateListeners.remove(listener);
    }

    /**
     * 添加群成员更新监听
     *
     * @param listener
     */
    public void addGroupMembersUpdateListener(OnGroupMembersUpdateListener listener) {
        if (listener != null) {
            groupMembersUpdateListeners.add(listener);
        }
    }

    /**
     * 删除群成员更新监听
     *
     * @param listener
     */
    public void removeGroupMembersUpdateListener(OnGroupMembersUpdateListener listener) {
        groupMembersUpdateListeners.remove(listener);
    }

    /**
     * 添加用户信息更新监听
     *
     * @param listener
     */
    public void addUserInfoUpdateListener(OnUserInfoUpdateListener listener) {
        if (listener == null) {
            return;
        }
        userInfoUpdateListeners.add(listener);
    }

    /**
     * 删除用户信息更新监听
     *
     * @param listener
     */
    public void removeUserInfoUpdateListener(OnUserInfoUpdateListener listener) {
        userInfoUpdateListeners.remove(listener);
    }

    /**
     * 添加好友状态更新监听
     *
     * @param listener
     */
    public void addFriendUpdateListener(OnFriendUpdateListener listener) {
        if (listener == null) {
            return;
        }
        friendUpdateListeners.add(listener);
    }

    /**
     * 删除好友状态监听
     *
     * @param listener
     */
    public void removeFriendUpdateListener(OnFriendUpdateListener listener) {
        friendUpdateListeners.remove(listener);
    }

    /**
     * 添加设置状态更新监听
     *
     * @param listener
     */
    public void addSettingUpdateListener(OnSettingUpdateListener listener) {
        if (listener == null) {
            return;
        }
        settingUpdateListeners.add(listener);
    }

    /**
     * 删除设置状态更监听
     *
     * @param listener
     */
    public void removeSettingUpdateListener(OnSettingUpdateListener listener) {
        settingUpdateListeners.remove(listener);
    }

    /**
     * 添加流量监听
     *
     * @param listener
     */
    public void addTrafficDataListener(OnTrafficDataListener listener) {
        if (listener == null) {
            return;
        }
        if (!onTrafficDataListeners.contains(listener)) {
            onTrafficDataListeners.add(listener);
        }
    }

    /**
     * 删除流量监听
     *
     * @param listener
     */
    public void removeTrafficDataListener(OnTrafficDataListener listener) {
        if (listener == null) {
            return;
        }
        onTrafficDataListeners.remove(listener);
    }

    /**
     * 设置平台是否是Pad平台。
     */
    public void setPlatform(boolean isPad) {
        this.isPad = isPad;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setPlatform(isPad);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Platform getPlatform() {
        if(isPad) {
            return Platform.PlatformType_APad;
        } else {
            return Platform.PlatformType_Android;
        }
    }

    /**
     * 启用国密加密，需要在connect之前调用，需要IM服务开启国密才可以使用。
     */
    public void useSM4() {
        useSM4 = true;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.useSM4();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启用AES256加密，需要在connect之前调用，需要IM服务开启AES256才可以使用。
     */
    public void useAES256() {
        useAES256 = true;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.useAES256();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用TCP的短链接。用在禁止HTTP的环境中。
     */
    public void useTcpShortLink() {
        tcpShortLink = true;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.useTcpShortLink();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 仅限 flutter 、uniapp 插件使用，请勿使用
     */
    public void useRawMsg() {
        rawMsg = true;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.useRawMsg();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否使用了TCP短链接
     *
     * @return 是否使用了TCP短链接
     */
    public boolean isTcpShortLink() {
        return tcpShortLink;
    }

    /**
     * 关闭消息搜索FTS功能，只能在connect前调用。一般不要用，除非有特殊情况。
     */
    public void noUseFts() {
        noUseFts = true;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.noUseFts();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置时间偏移。一般用于时间不正确的设备，可以设置时间偏移确保能够设备能够正常使用。时间是服务器时间-设备时间
     */
    public void setTimeOffset(int timeOffset) {
        this.timeOffset = timeOffset;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setTimeOffset(this.timeOffset);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启用签名验证，需要在connect之前调用，需要服务开启对应功能，详情请参考：https://docs.wildfirechat.cn/blogs/签名验证.html
     */
    public void checkSignature() {
        checkSignature = true;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.checkSignature();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置自定心跳间隔。单位是秒，取值范围为10-2400。一般不建议修改，除非是特殊场景。
     *
     * @param second 心跳间隔的秒数
     */
    public void setHeartBeatInterval(int second) {
        heartBeatInterval = second;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setHeartBeatInterval(second);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取clientId, 野火IM用clientId唯一表示用户设备
     */
    public synchronized String getClientId() {
        if (this.clientId != null) {
            return this.clientId;
        }

        String imei = null;
        SharedPreferences sp = gContext.getSharedPreferences("wfc_client_id", Context.MODE_PRIVATE);
        String spKey = "client_id";
        try (
            RandomAccessFile fw = new RandomAccessFile(gContext.getFilesDir().getAbsolutePath() + "/.wfcClientId", "rw");
        ) {
            fw.seek(0);
            imei = fw.readLine();
            if (TextUtils.isEmpty(imei)) {
                imei = sp.getString(spKey, null);
                if(TextUtils.isEmpty(imei)) {
                    imei = UUID.randomUUID().toString();
                    imei += System.currentTimeMillis();
                    sp.edit().putString(spKey, imei).apply();
                }
                fw.setLength(0);
                fw.seek(0);
                fw.writeBytes(imei);
            } else {
                String spimei = sp.getString(spKey, null);
                if(!imei.equals(spimei)) {
                    sp.edit().putString(spKey, imei).apply();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e("getClientError", ex.getMessage());
        } finally {
            if (TextUtils.isEmpty(imei)) {
                imei = sp.getString(spKey, null);
                if(TextUtils.isEmpty(imei)) {
                    imei = UUID.randomUUID().toString();
                    imei += System.currentTimeMillis();
                    sp.edit().putString(spKey, imei).apply();
                }
            }
        }
        this.clientId = imei;
        Log.d(TAG, "clientId " + this.clientId);
        return imei;
    }

    /**
     * 创建频道
     *
     * @param channelId       频道id，如果传null，野火会自动生成id；否则，使用用户提供的id，需要保证此id的唯一性
     * @param channelName     频道名称
     * @param channelPortrait 频道头像的网络地址
     * @param desc            频道描述
     * @param extra           额外信息，可用于存储一些应用相关信息
     * @param callback        创建频道结果的回调
     */
    public void createChannel(@Nullable String channelId, String channelName, String channelPortrait, String desc, String extra, final GeneralCallback2 callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.createChannel(channelId, channelName, channelPortrait, desc, extra, new ICreateChannelCallback.Stub() {
                @Override
                public void onSuccess(ChannelInfo channelInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(channelInfo.channelId));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 修改频道信息
     *
     * @param channelId  频道id
     * @param modifyType 修改类型，标识修改频道的什么信息
     * @param newValue   修改目标值
     * @param callback   修改结果回调
     */
    public void modifyChannelInfo(String channelId, ModifyChannelInfoType modifyType, String newValue, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.modifyChannelInfo(channelId, modifyType.ordinal(), newValue, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }


    /**
     * 获取频道信息
     *
     * @param channelId
     * @param refresh   是否刷新频道信息。为true时，会从服务器拉取最新的频道信息，如果有更新，则会通过{@link OnChannelInfoUpdateListener}回调更新后的信息
     * @return 频道信息，可能为null
     */
    public @Nullable
    ChannelInfo getChannelInfo(String channelId, boolean refresh) {
        if (!checkRemoteService()) {
            return new NullChannelInfo(channelId);
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            return new NullChannelInfo(channelId);
        }

        try {
            ChannelInfo channelInfo = mClient.getChannelInfo(channelId, refresh);
            if (channelInfo == null) {
                channelInfo = new NullChannelInfo(channelId);
            }
            return channelInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 搜索频道，仅在本地搜索
     *
     * @param keyword  搜索关键字
     * @param callback 搜索结果回调
     */
    public void searchChannel(String keyword, SearchChannelCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(keyword)) {
            Log.e(TAG, "Error, keyword is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.searchChannel(keyword, new cn.wildfirechat.client.ISearchChannelCallback.Stub() {

                @Override
                public void onSuccess(final List<ChannelInfo> channelInfos) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(channelInfos);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 判断是否已经收听该频道
     *
     * @param channelId
     * @return true, 已收听；false，为收听
     */
    public boolean isListenedChannel(String channelId) {
        if (!checkRemoteService()) {
            return false;
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            return false;
        }

        try {
            return mClient.isListenedChannel(channelId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 收听或取消收听频道
     *
     * @param channelId
     * @param listen    true，收听；false，取消收听
     * @param callback  操作结果回调
     */
    public void listenChannel(String channelId, boolean listen, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.listenChannel(channelId, listen, new cn.wildfirechat.client.IGeneralCallback.Stub() {

                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 销毁频道
     *
     * @param channelId
     * @param callback
     */
    public void destoryChannel(String channelId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.destoryChannel(channelId, new cn.wildfirechat.client.IGeneralCallback.Stub() {

                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 获取我创建的频道id列表
     *
     * @return
     */
    public List<String> getMyChannels() {
        if (!checkRemoteService()) {
            return new ArrayList<>();
        }

        try {
            return mClient.getMyChannels();
        } catch (RemoteException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 获取我收听的频道id列表
     *
     * @return
     */
    public List<String> getListenedChannels() {
        if (!checkRemoteService()) {
            return new ArrayList<>();
        }

        try {
            return mClient.getListenedChannels();
        } catch (RemoteException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 获取我收听的频道id列表
     *
     * @return
     */
    public void getRemoteListenedChannels(GeneralCallback3 callback3) {
        if (!checkRemoteService()) {
            callback3.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getRemoteListenedChannels(new cn.wildfirechat.client.IGeneralCallback3.Stub() {
                @Override
                public void onSuccess(List<String> results) throws RemoteException {
                    mainHandler.post(() -> callback3.onSuccess(results));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback3.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            callback3.onFail(ErrorCode.SERVICE_DIED);
        }
    }

    /**
     * 获取外域信息
     *
     * @param domainId
     * @param refresh  是否刷新域信息。为true时，会从服务器拉取最新的域信息，如果有更新，则会通过{@link OnDomainInfoUpdateListener}回调更新后的信息
     * @return 域信息，可能为null
     */
    public @Nullable
    DomainInfo getDomainInfo(String domainId, boolean refresh) {
        if (!checkRemoteService()) {
            return new NullDomainInfo(domainId);
        }
        if (TextUtils.isEmpty(domainId)) {
            Log.e(TAG, "Error, channelId is empty");
            return new NullDomainInfo(domainId);
        }

        try {
            DomainInfo domainInfo = mClient.getDomainInfo(domainId, refresh);
            if (domainInfo == null) {
                domainInfo = new NullDomainInfo(domainId);
            }
            return domainInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取域列表，关于域请参考 https://docs.wildfirechat.cn/blogs/野火IM互通方案.html
     *
     * @callback 回调返回域列表。
     */
    public void loadRemoteDomains(GetRemoteDomainsCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.getRemoteDomainInfos(new IGetRemoteDomainInfosCallback.Stub() {
                @Override
                public void onSuccess(List<DomainInfo> domainInfos) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(domainInfos));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加会话更新监听
     *
     * @param listener
     */
    public void addConversationInfoUpdateListener(OnConversationInfoUpdateListener listener) {
        if (listener == null) {
            return;
        }
        conversationInfoUpdateListeners.add(listener);
    }

    /**
     * 删除会话监听
     *
     * @param listener
     */
    public void removeConversationInfoUpdateListener(OnConversationInfoUpdateListener listener) {
        conversationInfoUpdateListeners.remove(listener);
    }

    /**
     * 添加消息撤回监听
     *
     * @param listener
     */
    public void addRecallMessageListener(OnRecallMessageListener listener) {
        if (listener == null) {
            return;
        }
        recallMessageListeners.add(listener);
    }

    /**
     * 删除消息撤回监听
     *
     * @param listener
     */
    public void removeRecallMessageListener(OnRecallMessageListener listener) {
        recallMessageListeners.remove(listener);
    }

    /**
     * 添加消息删除监听
     *
     * @param listener
     */
    public void addDeleteMessageListener(OnDeleteMessageListener listener) {
        if (listener == null) {
            return;
        }
        deleteMessageListeners.add(listener);
    }

    /**
     * 删除消息删除监听
     *
     * @param listener
     */
    public void removeDeleteMessageListener(OnDeleteMessageListener listener) {
        deleteMessageListeners.remove(listener);
    }

    /**
     * 添加频道更新监听
     *
     * @param listener
     */
    public void addChannelInfoUpdateListener(OnChannelInfoUpdateListener listener) {
        if (listener == null) {
            return;
        }
        channelInfoUpdateListeners.add(listener);
    }

    /**
     * 删除频道信息更新监听
     *
     * @param listener
     */
    public void removeChannelInfoListener(OnChannelInfoUpdateListener listener) {
        channelInfoUpdateListeners.remove(listener);
    }

    /**
     * 添加消息更新监听
     *
     * @param listener
     */
    public void addOnMessageUpdateListener(OnMessageUpdateListener listener) {
        if (listener == null) {
            return;
        }
        messageUpdateListeners.add(listener);
    }

    /**
     * 删除消息更新监听
     *
     * @param listener
     */
    public void removeOnMessageUpdateListener(OnMessageUpdateListener listener) {
        messageUpdateListeners.remove(listener);
    }

    /**
     * 添加删除消息监听
     *
     * @param listener
     */
    public void addClearMessageListener(OnClearMessageListener listener) {
        if (listener == null) {
            return;
        }

        clearMessageListeners.add(listener);
    }

    /**
     * 移除删除消息监听
     *
     * @param listener
     */
    public void removeClearMessageListener(OnClearMessageListener listener) {
        clearMessageListeners.remove(listener);
    }

    /**
     * 添加删除会话监听
     *
     * @param listener
     */
    public void addRemoveConversationListener(OnRemoveConversationListener listener) {
        if (listener == null) {
            return;
        }
        removeConversationListeners.add(listener);
    }

    /**
     * 移除删除会话监听
     *
     * @param listener
     */
    public void removeRemoveConversationListener(OnRemoveConversationListener listener) {
        removeConversationListeners.remove(listener);
    }


    /**
     * 添加im服务进程监听监听
     *
     * @param listener
     */
    public void addIMServiceStatusListener(IMServiceStatusListener listener) {
        if (listener == null) {
            return;
        }
        imServiceStatusListeners.add(listener);
    }

    /**
     * 移除im服务进程状态监听
     *
     * @param listener
     */
    public void removeIMServiceStatusListener(IMServiceStatusListener listener) {
        imServiceStatusListeners.remove(listener);
    }

    /**
     * 添加消息已送达监听
     *
     * @param listener
     */
    public void addMessageDeliverListener(OnMessageDeliverListener listener) {
        if (listener == null) {
            return;
        }
        messageDeliverListeners.add(listener);
    }

    /**
     * 移除消息已送达监听
     *
     * @param listener
     */
    public void removeMessageDeliverListener(OnMessageDeliverListener listener) {
        messageDeliverListeners.remove(listener);
    }

    /**
     * 添加消息已读监听
     *
     * @param listener
     */
    public void addMessageReadListener(OnMessageReadListener listener) {
        if (listener == null) {
            return;
        }
        messageReadListeners.add(listener);
    }

    /**
     * 移除消息已读监听
     *
     * @param listener
     */
    public void removeMessageReadListener(OnMessageReadListener listener) {
        messageReadListeners.remove(listener);
    }


    public void addConferenceEventListener(OnConferenceEventListener listener) {
        if (listener == null) {
            return;
        }
        conferenceEventListeners.add(listener);
    }

    public void removeConferenceEventListener(OnConferenceEventListener listener) {
        conferenceEventListeners.remove(listener);
    }

    /**
     * 添加用户在线状态变化监听, 记得调用{@link #removeUserOnlineEventListener(OnUserOnlineEventListener)}删除监听
     *
     * @param listener
     */
    public void addUserOnlineEventListener(OnUserOnlineEventListener listener) {
        if (listener == null) {
            return;
        }
        userOnlineEventListeners.add(listener);
    }

    /**
     * 移除用户在线状态变化监听
     *
     * @param listener
     */
    public void removeUserOnlineEventListener(OnUserOnlineEventListener listener) {
        userOnlineEventListeners.remove(listener);
    }

    /**
     * 添加密聊状态变化监听, 记得调用{@link #removeSecretChatStateChangedListener(SecretChatStateChangeListener)}删除监听
     *
     * @param listener
     */
    public void addSecretChatStateChangedListener(SecretChatStateChangeListener listener) {
        if (listener == null) {
            return;
        }
        secretChatStateChangeListeners.add(listener);
    }

    /**
     * 移除密聊状态变化监听
     *
     * @param listener
     */
    public void removeSecretChatStateChangedListener(SecretChatStateChangeListener listener) {
        secretChatStateChangeListeners.remove(listener);
    }

    /**
     * 添加密聊消息焚毁状态监听, 记得调用{@link #removeSecretMessageBurnStateListener(SecretMessageBurnStateListener)}删除监听
     *
     * @param listener
     */
    public void addSecretMessageBurnStateListener(SecretMessageBurnStateListener listener) {
        if (listener == null) {
            return;
        }
        secretMessageBurnStateListeners.add(listener);
    }

    /**
     * 移除密聊消息焚毁状态监听
     *
     * @param listener
     */
    public void removeSecretMessageBurnStateListener(SecretMessageBurnStateListener listener) {
        secretMessageBurnStateListeners.remove(listener);
    }

    /**
     * 添加域信息变化监听, 记得调用{@link #removeDomainInfoUpdateListener(OnDomainInfoUpdateListener)}删除监听
     *
     * @param listener
     */
    public void addDomainInfoUpdateListener(OnDomainInfoUpdateListener listener) {
        if (listener == null) {
            return;
        }
        domainInfoUpdateListeners.add(listener);
    }

    /**
     * 移除域信息变化监听
     *
     * @param listener
     */
    public void removeDomainInfoUpdateListener(OnDomainInfoUpdateListener listener) {
        domainInfoUpdateListeners.remove(listener);
    }

    private void validateMessageContent(Class<? extends MessageContent> msgContentClazz) {
        String className = msgContentClazz.getName();
        try {
            Constructor c = msgContentClazz.getConstructor();
            if (c.getModifiers() != Modifier.PUBLIC) {
                throw new IllegalArgumentException(className + ", the default constructor of your custom messageContent class should be public，自定义消息的构造函数必须是public的，请参考TextMessageContent.java");
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(className + ", custom messageContent class must have a default constructor，自定义消息必须要有一个默认的无参构造函数，请参考TextMessageContent.java");
        }

        // 建议打开，以便对自定义消息的合法性进行检查
//        try {
//            msgContentClazz.getDeclaredMethod("encode");
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            throw new IllegalArgumentException(className + ", custom messageContent class must override encode，自定义消息必须覆盖encode方法，并调用super.encode()，请参考TextMessageContent.java");
//        }

        try {
            Field creator = msgContentClazz.getDeclaredField("CREATOR");
            if ((creator.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == 0) {
                throw new IllegalArgumentException(className + ", custom messageContent class implements Parcelable but does not provide a CREATOR field，自定义消息必须实现Parcelable接口，并提供一个CREATOR，请参考TextMessageContent.java");
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(className + ", custom messageContent class implements Parcelable but does not provide a CREATOR field，自定义消息必须实现Parcelable接口，并且提供一个CREATOR，请参考TextMessageContent.java");
        }

        try {
            msgContentClazz.getDeclaredMethod("writeToParcel", Parcel.class, int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(className + ", custom messageContent class must override writeToParcel，自定义消息必须覆盖writeToParcel方法，请参考TextMessageContent.java");
        }

        ContentTag tag = msgContentClazz.getAnnotation(ContentTag.class);
        if (tag == null) {
            throw new IllegalArgumentException(className + ", custom messageContent class must have a ContentTag annotation，自定义消息类必须包含ContentTag注解，请参考TextMessageContent.java");
        }

        if (tag.type() == 0 && !msgContentClazz.equals(UnknownMessageContent.class)) {
            throw new IllegalArgumentException(className + ", custom messageContent class's ContentTag annotation must set the type value，自定消息类的ContentTag注解，type值不能为默认，请参考TextMessageContent.java");
        }
    }

    /**
     * 注册自自定义消息
     *
     * @param msgContentCls 自定义消息实现类，可参考自定义消息文档
     */
    public void registerMessageContent(Class<? extends MessageContent> msgContentCls) {

        validateMessageContent(msgContentCls);
        ContentTag tag = (ContentTag) msgContentCls.getAnnotation(ContentTag.class);
        messageContentMap.put(tag.type(), msgContentCls);
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.registerMessageContent(msgContentCls.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册自定义消息的存储类型
     * 给 uniapp 原生插件使用
     *
     * @param type 消息类型
     * @param flag 消息存储类型
     */
    public void registerMessageFlag(int type, PersistFlag flag) {
        messagePersistFlagMap.put(type, flag);
        if (!checkRemoteService()) {
            return;
        }
        try {
            mClient.registerMessageFlag(type, flag.getValue());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 插入消息
     *
     * @param conversation 目标会话
     * @param sender       消息发送者id
     * @param content      消息体
     * @param status       消息状态
     * @param notify       是否通知界面，通知时，会通过{@link #onReceiveMessage(List, boolean)}通知界面
     * @param toUsers      定向发送给会话中的某些用户；为空，则发给所有人
     * @param serverTime   服务器时间
     * @return
     */
    public Message insertMessage(Conversation conversation, String sender, MessageContent content, MessageStatus status, boolean notify, String[] toUsers, long serverTime) {
        return insertMessage(conversation, sender, 0, content, status, notify, toUsers, serverTime);
    }

    /**
     * 插入消息
     *
     * @param conversation 目标会话
     * @param sender       消息发送者id
     * @param messageUid   消息uid，可以传0
     * @param content      消息体
     * @param status       消息状态
     * @param notify       是否通知界面，通知时，会通过{@link #onReceiveMessage(List, boolean)}通知界面
     * @param toUsers      定向发送给会话中的某些用户；为空，则发给所有人
     * @param serverTime   服务器时间
     * @return
     */
    public Message insertMessage(Conversation conversation, String sender, long messageUid, MessageContent content, MessageStatus status, boolean notify, String[] toUsers, long serverTime) {
        if (!checkRemoteService()) {
            return null;
        }

        Message message = new Message();
        message.conversation = conversation;
        message.content = content;
        message.status = status;
        message.messageUid = messageUid;
        message.serverTime = serverTime;
        message.toUsers = toUsers;

        message.direction = MessageDirection.Send;
        if (status.value() >= MessageStatus.Mentioned.value()) {
            message.direction = MessageDirection.Receive;
            if (conversation.type == Conversation.ConversationType.Single) {
                message.sender = conversation.target;
            } else {
                message.sender = sender;
            }
        } else {
            message.sender = getUserId();
        }

        try {
            message = mClient.insertMessage(message, notify);
            if (notify) {
                onReceiveMessage(Collections.singletonList(message), false);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }

        return message;
    }

    /**
     * 更新消息内容。不支持聊天室，聊天室消息本地不存储，所有无法更新。
     *
     * @param messageId     消息id
     * @param newMsgContent 新的消息体，未更新部分，不可置空！
     * @return
     */
    public boolean updateMessage(long messageId, MessageContent newMsgContent) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            Message message = mClient.getMessage(messageId);
            if (message == null) {
                Log.e(TAG, "update message failure, message not exist");
                return false;
            }
            message.content = newMsgContent;
            boolean result = mClient.updateMessageContent(message);
            mainHandler.post(() -> {
                for (OnMessageUpdateListener listener : messageUpdateListeners) {
                    listener.onMessageUpdate(message);
                }
            });
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 更新消息内容和时间
     *
     * @param messageId     消息id
     * @param newMsgContent 新的消息体，未更新部分，不可置空！
     * @param timestamp     时间戳
     * @return
     */
    public boolean updateMessage(long messageId, MessageContent newMsgContent, long timestamp) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            Message message = mClient.getMessage(messageId);
            if (message == null) {
                Log.e(TAG, "update message failure, message not exist");
                return false;
            }
            message.content = newMsgContent;
            message.serverTime = timestamp;

            boolean result = mClient.updateMessageContentAndTime(message);
            mainHandler.post(() -> {
                for (OnMessageUpdateListener listener : messageUpdateListeners) {
                    listener.onMessageUpdate(message);
                }
            });
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 更新消息状态。一般情况下协议栈会自动处理好状态，不建议手动处理状态。
     *
     * @param messageId 消息id
     * @param status    新的消息状态，需要与消息方向对应。
     * @return
     */
    public boolean updateMessage(long messageId, MessageStatus status) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            Message message = mClient.getMessage(messageId);
            if (message == null) {
                Log.e(TAG, "update message failure, message not exist");
                return false;
            }
            message.status = status;

//            if ((message.direction == MessageDirection.Send && status.value() >= MessageStatus.Mentioned.value()) ||
//                    message.direction == MessageDirection.Receive && status.value() < MessageStatus.Mentioned.value()) {
//                return false;
//            }

            boolean result = mClient.updateMessageStatus(messageId, status.value());
            mainHandler.post(() -> {
                for (OnMessageUpdateListener listener : messageUpdateListeners) {
                    listener.onMessageUpdate(message);
                }
            });
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置Lite模式。
     * <p>
     * Lite模式下，协议栈不存储数据库，不同步所有信息，只能收发消息，接收消息只接收连接以后发送的消息。
     * 此函数只能在connect之前调用。
     *
     * @param isLiteMode 是否是Lite模式
     */
    public void setLiteMode(boolean isLiteMode) {
        this.isLiteMode = isLiteMode;
        if (mClient != null) {
            try {
                mClient.setLiteMode(isLiteMode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置低速模式。
     * <p>
     * 低速模式首次登录不同步历史消息，不同步设置，一般用户极窄带宽设备下。
     * 此函数只能在connect之前调用。
     *
     * @param isLowBPSMode 是否是低速模式
     */
    public void setLowBPSMode(boolean isLowBPSMode) {
        this.isLowBPSMode = isLowBPSMode;
        if (mClient != null) {
            try {
                mClient.setLowBPSMode(isLowBPSMode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 连接服务器
     * userId和token都不允许为空
     * 需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
     * 另外不能多次connect，如果需要切换用户请先disconnect，然后3秒钟之后再connect（如果是用户手动登录可以不用等，因为用户操作很难3秒完成，如果程序自动切换请等3秒）
     *
     * @param userId
     * @param token
     * @return 返回上一次活动时间。如果间隔时间较长，可以加个第一次登录的等待提示界面，在等待时同步所有的用户信息/群组信息/频道信息等。
     */
    public long connect(String userId, String token) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(token) || TextUtils.isEmpty(SERVER_HOST)) {
            throw new IllegalArgumentException("userId, token and im_server_host must not be empty!");
        }
        this.userId = userId;
        this.token = token;

        if (mClient != null) {
            try {
                Log.d(TAG, "connect " + userId + " " + token);
                return mClient.connect(this.userId, this.token);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Mars service not start yet!");
        }
        return 0;
    }

    /**
     * 主动断开连接
     *
     * @param disablePush  是否停止推送，在cleanSession为true时无意义
     * @param cleanSession 是否清除会话session，清除之后，所有之前的会话信息会被删除
     */
    public void disconnect(boolean disablePush, boolean cleanSession) {
        if (mClient != null) {
            try {
                Log.d(TAG, "disconnect " + disablePush + " " + cleanSession);
                mClient.disconnect(disablePush, cleanSession);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            this.userId = null;
            this.token = null;
        }
    }

    /**
     * 设置备选服务地址，仅专业版支持，一般用于政企单位内外网两种网络环境。
     *
     * @param strategy 网络策略，0是复合连接；1是使用主要网络；2使用备选网络
     */
    public void setBackupAddressStrategy(int strategy) {
        backupAddressStrategy = strategy;

        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setBackupAddressStrategy(strategy);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置服务地址
     *
     * @param imServerHost IM服务地址
     */
    public void setIMServerHost(String imServerHost) {
        SERVER_HOST = imServerHost;
        if (mClient != null) {
            try {
                mClient.setServerAddress(imServerHost);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置备选服务地址，仅专业版支持，一般用于政企单位内外网两种网络环境。
     *
     * @param host 用备选网络ip
     * @param port 用备选网络端口
     */
    public void setBackupAddress(String host, int port) {
        backupAddressHost = host;
        backupAddressPort = port;

        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setBackupAddress(host, port);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 双网环境时，获取当前连接的网络类型
     *
     * @return 1 主网络；-1 备选网络；0 未知
     */
    public int getConnectedNetworkType() {
        if (!checkRemoteService()) {
            return 0;
        }
        try {
            return mClient.getConnectedNetworkType();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 设置协议栈短连接UA。
     *
     * @param userAgent 协议栈短连接使用的UA
     */
    public void setProtoUserAgent(String userAgent) {
        protoUserAgent = userAgent;

        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setProtoUserAgent(userAgent);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加协议栈短连接自定义Header
     *
     * @param header 协议栈短连接使用的UA
     * @param value  协议栈短连接使用的UA
     */
    public void addHttpHeader(String header, String value) {
        if (!TextUtils.isEmpty(value)) {
            protoHttpHeaderMap.put(header, value);
        }

        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.addHttpHeader(header, value);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送已经保存的消息
     *
     * @param msg
     * @param expireDuration
     * @param callback
     */
    public void sendSavedMessage(Message msg, int expireDuration, SendMessageCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                msg.status = MessageStatus.Send_Failure;
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            for (OnSendMessageListener listener : sendMessageListeners) {
                listener.onSendFail(msg, ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.sendSavedMessage(msg, expireDuration, new cn.wildfirechat.client.ISendMessageCallback.Stub() {
                @Override
                public void onSuccess(long messageUid, long timestamp) throws RemoteException {
                    msg.messageUid = messageUid;
                    msg.serverTime = timestamp;
                    msg.status = MessageStatus.Sent;
                    if (msg.content instanceof MediaMessageContent && urlRedirector != null) {
                        String remoteUrl = ((MediaMessageContent) msg.content).remoteUrl;
                        ((MediaMessageContent) msg.content).remoteUrl = urlRedirector.urlRedirect(remoteUrl);
                    }
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess(messageUid, timestamp);
                            }
                            for (OnSendMessageListener listener : sendMessageListeners) {
                                listener.onSendSuccess(msg);
                            }
                        }
                    });
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    msg.status = MessageStatus.Send_Failure;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onFail(errorCode);
                            }
                            for (OnSendMessageListener listener : sendMessageListeners) {
                                listener.onSendFail(msg, errorCode);
                            }
                        }
                    });
                }

                @Override
                public void onPrepared(final long messageId, final long savedTime) throws RemoteException {
                    msg.messageId = messageId;
                    msg.serverTime = savedTime;
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onPrepare(messageId, savedTime);
                        }
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onSendPrepare(msg, savedTime);
                        }
                    });
                }

                @Override
                public void onProgress(final long uploaded, final long total) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onProgress(uploaded, total));
                    }

                    mainHandler.post(() -> {
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onProgress(msg, uploaded, total);
                        }
                    });
                }

                @Override
                public void onMediaUploaded(final String remoteUrl) throws RemoteException {
                    if (msg.content instanceof MediaMessageContent) {
                        MediaMessageContent mediaMessageContent = (MediaMessageContent) msg.content;
                        mediaMessageContent.remoteUrl = remoteUrl;
                    } else if (msg.content instanceof RawMessageContent) {
                        RawMessageContent rawMessageContent = (RawMessageContent) msg.content;
                        rawMessageContent.payload.remoteMediaUrl = remoteUrl;
                    }
                    if (msg.messageId == 0) {
                        return;
                    }
                    if (callback != null) {
                        mainHandler.post(() -> callback.onMediaUpload(remoteUrl));
                    }
                    mainHandler.post(() -> {
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onMediaUpload(msg, remoteUrl);
                        }
                    });
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息
     *
     * @param conversation
     * @param content
     * @param toUsers        定向发送给会话中的某些用户；为空，则发给所有人
     * @param expireDuration
     * @param callback
     */
    public void sendMessage(Conversation conversation, MessageContent content, String[] toUsers, int expireDuration, SendMessageCallback callback) {
        Message msg = new Message();
        msg.conversation = conversation;
        msg.content = content;
        msg.toUsers = toUsers;
        sendMessage(msg, expireDuration, callback);
    }

    /**
     * 发送消息
     *
     * @param msg
     * @param callback 发送消息状态回调
     */
    public void sendMessage(final Message msg, final SendMessageCallback callback) {
        sendMessage(msg, 0, callback);
    }

    /**
     * 发送消息
     *
     * @param msg            消息
     * @param callback       发送状态回调
     * @param expireDuration 0, 永不过期；否则，规定时间内，对方未收到，则丢弃；单位是毫秒
     */
    public void sendMessage(final Message msg, final int expireDuration, final SendMessageCallback callback) {
        msg.direction = MessageDirection.Send;
        msg.status = MessageStatus.Sending;
        msg.serverTime = System.currentTimeMillis();
        msg.sender = userId;
        msg.messageUid = 0;
        if (!checkRemoteService()) {
            if (callback != null) {
                msg.status = MessageStatus.Send_Failure;
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            for (OnSendMessageListener listener : sendMessageListeners) {
                listener.onSendFail(msg, ErrorCode.SERVICE_DIED);
            }
            return;
        }

        MediaMessageUploadCallback mediaMessageUploadCallback = null;
        if (msg.content instanceof MediaMessageContent || (msg.content instanceof RawMessageContent && !TextUtils.isEmpty(((RawMessageContent) msg.content).payload.localMediaPath))) {
            String remoteUrl;
            String localPath;
            if (msg.content instanceof MediaMessageContent) {
                remoteUrl = ((MediaMessageContent) msg.content).remoteUrl;
                localPath = ((MediaMessageContent) msg.content).localPath;
            } else {
                remoteUrl = ((RawMessageContent) msg.content).payload.remoteMediaUrl;
                localPath = ((RawMessageContent) msg.content).payload.localMediaPath;
            }

            if (TextUtils.isEmpty(remoteUrl)) {
                if (!TextUtils.isEmpty(localPath)) {
                    File file = new File(localPath);
                    if (!file.exists()) {
                        if (callback != null) {
                            callback.onFail(ErrorCode.FILE_NOT_EXIST);
                        }
                        return;
                    }

                    if (file.length() >= 100 * 1024 * 1024 && (!isSupportBigFilesUpload() || msg.conversation.type == Conversation.ConversationType.SecretChat)) {
                        if (callback != null) {
                            callback.onFail(ErrorCode.FILE_TOO_LARGE);
                        }
                        return;
                    }
                }
            }
        } else if (sendLogCommand != null && (msg.content instanceof TextMessageContent || (rawMsg && msg.content instanceof RawMessageContent))) {
            String text = "";
            if (msg.content instanceof TextMessageContent) {
                text = ((TextMessageContent) msg.content).getContent();
            } else if (msg.content instanceof RawMessageContent) {
                text = ((RawMessageContent) msg.content).payload.searchableContent;
            }

            if (sendLogCommand.equals(text)) {
                if (!startLog) {
                    ((TextMessageContent) msg.content).setContent("未开启日志，无法发送日志");
                } else {
                    List<String> logFilesPath = getLogFilesPath();
                    if (!logFilesPath.isEmpty()) {
                        FileMessageContent fileMessageContent = new FileMessageContent(logFilesPath.get(logFilesPath.size() - 1));
                        msg.content = fileMessageContent;

                        mediaMessageUploadCallback = new MediaMessageUploadCallback() {
                            @Override
                            public void onMediaMessageUploaded(String remoteUrl) {
                                TextMessageContent textMessageContent = new TextMessageContent(remoteUrl);
                                sendMessage(msg.conversation, textMessageContent, null, 0, null);
                            }
                        };
                    } else {
                        ((TextMessageContent) msg.content).setContent("日志为空，无法发送日志");
                    }
                }
            }
        }

        try {
            MediaMessageUploadCallback finalMediaMessageUploadCallback = mediaMessageUploadCallback;
            mClient.send(msg, new cn.wildfirechat.client.ISendMessageCallback.Stub() {
                @Override
                public void onSuccess(final long messageUid, final long timestamp) throws RemoteException {
                    msg.messageUid = messageUid;
                    msg.serverTime = timestamp;
                    msg.status = MessageStatus.Sent;
                    if (msg.content instanceof MediaMessageContent && urlRedirector != null) {
                        String remoteUrl = ((MediaMessageContent) msg.content).remoteUrl;
                        ((MediaMessageContent) msg.content).remoteUrl = urlRedirector.urlRedirect(remoteUrl);
                    }
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess(messageUid, timestamp);
                            }
                            for (OnSendMessageListener listener : sendMessageListeners) {
                                listener.onSendSuccess(msg);
                            }
                        }
                    });
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    msg.status = MessageStatus.Send_Failure;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onFail(errorCode);
                            }
                            for (OnSendMessageListener listener : sendMessageListeners) {
                                listener.onSendFail(msg, errorCode);
                            }
                        }
                    });
                }

                @Override
                public void onPrepared(final long messageId, final long savedTime) throws RemoteException {
                    msg.messageId = messageId;
                    msg.serverTime = savedTime;
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onPrepare(messageId, savedTime);
                        }
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onSendPrepare(msg, savedTime);
                        }
                    });
                }

                @Override
                public void onProgress(final long uploaded, final long total) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onProgress(uploaded, total));
                    }

                    mainHandler.post(() -> {
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onProgress(msg, uploaded, total);
                        }
                    });
                }

                @Override
                public void onMediaUploaded(final String remoteUrl) throws RemoteException {
                    if (msg.content instanceof MediaMessageContent) {
                        MediaMessageContent mediaMessageContent = (MediaMessageContent) msg.content;
                        mediaMessageContent.remoteUrl = remoteUrl;
                    } else if (msg.content instanceof RawMessageContent) {
                        RawMessageContent rawMessageContent = (RawMessageContent) msg.content;
                        rawMessageContent.payload.remoteMediaUrl = remoteUrl;
                    }
                    if (msg.messageId == 0) {
                        return;
                    }

                    if (finalMediaMessageUploadCallback != null) {
                        finalMediaMessageUploadCallback.onMediaMessageUploaded(remoteUrl);
                    }

                    if (callback != null) {
                        mainHandler.post(() -> callback.onMediaUpload(remoteUrl));
                    }
                    mainHandler.post(() -> {
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onMediaUpload(msg, remoteUrl);
                        }
                    });
                }
            }, expireDuration);
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                msg.status = MessageStatus.Send_Failure;
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
            mainHandler.post(() -> {
                for (OnSendMessageListener listener : sendMessageListeners) {
                    listener.onSendFail(msg, ErrorCode.SERVICE_EXCEPTION);
                }
            });
        }
    }

    /**
     * 取消消息发送，只有媒体类消息才可以取消发送，普通消息发送中时无法取消。
     *
     * @param messageId
     * @return 是否取消成功
     */
    public boolean cancelSendingMessage(long messageId) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.cancelSendingMessage(messageId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 消息撤回
     *
     * @param msg      想撤回的消息
     * @param callback 撤回回调
     */
    public void recallMessage(Message msg, final GeneralCallback callback) {
        try {
            mClient.recall(msg.messageUid, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    Message recallMsg = mClient.getMessage(msg.messageId);
                    if (recallMsg != null) {
                        msg.content = recallMsg.content;
                        msg.sender = recallMsg.sender;
                        msg.serverTime = recallMsg.serverTime;
                        msg.messageUid = recallMsg.messageUid;
                    } else {
                        MessagePayload payload = msg.content.encode();
                        RecallMessageContent recallCnt = new RecallMessageContent();
                        recallCnt.setOperatorId(userId);
                        recallCnt.setMessageUid(msg.messageUid);
                        recallCnt.fromSelf = true;
                        recallCnt.setOriginalSender(msg.sender);
                        recallCnt.setOriginalContent(payload.content);
                        recallCnt.setOriginalContentType(payload.type);
                        recallCnt.setOriginalExtra(payload.extra);
                        recallCnt.setOriginalSearchableContent(payload.searchableContent);
                        recallCnt.setOriginalMessageTimestamp(msg.serverTime);
                        msg.content = recallCnt;
                        msg.sender = userId;
                        msg.serverTime = System.currentTimeMillis();
                    }
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        for (OnRecallMessageListener listener : recallMessageListeners) {
                            listener.onRecallMessage(msg);
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关掉协议栈服务
     */
    public void shutdown() {
        Log.d(TAG, "shutdown");
        if (mClient != null) {
            gContext.unbindService(serviceConnection);
        }
    }

    /**
     * 工作线程handler
     *
     * @return
     */
    public Handler getWorkHandler() {
        return workHandler;
    }

    /**
     * 获取主线程handler
     *
     * @return
     */
    public Handler getMainHandler() {
        return mainHandler;
    }

    /**
     * 获取会话列表
     * <p>
     * 由于 ipc 大小限制，有丢失会话风险，建议使用 {@link ChatManager#getConversationListAsync}
     *
     * @param conversationTypes 获取哪些类型的会话
     * @param lines             获取哪些会话线路
     * @return
     */
    @NonNull
    public List<ConversationInfo> getConversationList(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        return getConversationList(conversationTypes, lines, true);
    }

    /**
     * 获取比 timestamp 更新的会话列表
     *
     * @param conversationTypes
     * @param lines
     * @param lastMessage       是否包含 lastMessage信息
     * @return
     */
    public List<ConversationInfo> getConversationList(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, boolean lastMessage) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return new ArrayList<>();
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
            lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type and lines");
            return new ArrayList<>();
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            return mClient.getConversationList(intypes, inlines, lastMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * 获取会话列表
     *
     * @param conversationTypes 获取哪些类型的会话
     * @param lines             获取哪些会话线路
     * @param callback          会话列表回调
     */
    public void getConversationListAsync(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, GetConversationListCallback callback) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        if (callback == null) {
            return;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
            lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type and lines");
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        internalWorkHandler.post(() -> {
            try {
                List<ConversationInfo> convs = new ArrayList<>();
                mClient.getConversationListAsync(intypes, inlines, new IGetConversationListCallback.Stub() {
                    @Override
                    public void onSuccess(List<ConversationInfo> infos, boolean hasMore) throws RemoteException {
                        convs.addAll(infos);
                        if (!hasMore) {
                            mainHandler.post(() -> {
                                callback.onSuccess(convs);
                            });
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) throws RemoteException {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
                callback.onFail(-1);
            }
        });
    }

    /**
     * 获取会话信息
     *
     * @param conversation
     * @return
     */
    public @Nullable
    ConversationInfo getConversation(Conversation conversation) {
        ConversationInfo conversationInfo = null;
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return null;
        }

        try {
            conversationInfo = mClient.getConversation(conversation.type.getValue(), conversation.target, conversation.line);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        conversationInfo = conversationInfo != null ? conversationInfo : new NullConversationInfo(conversation);
        return conversationInfo;
    }

    /**
     * 获取会话中最早一条未读消息的消息ID
     *
     * @param conversation 会话
     * @return 最早一条未读消息的消息ID
     */
    public long getFirstUnreadMessageId(Conversation conversation) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return 0L;
        }

        try {
            return mClient.getFirstUnreadMessageId(conversation.type.getValue(), conversation.target, conversation.line);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return 0L;
    }

    /**
     * 获取会话消息
     *
     * @param conversation 会话
     * @param fromIndex    消息起始id(messageId)
     * @param before       true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count        获取消息条数
     * @param withUser     只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @return 由于ipc大小限制，本接口获取到的消息列表可能不完整，并且超级群里面，可能返回为完全加载的消息，请使用异步获取
     */
    @Deprecated
    public List<Message> getMessages(Conversation conversation, long fromIndex, boolean before, int count, String withUser) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMessages(conversation, fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取某类会话消息
     *
     * @param conversationTypes 会话类型列表
     * @param lines             会话线路列表
     * @param contentTypes      过滤消息类型，如果全部消息请为空
     * @param fromIndex         消息起始id(messageId)
     * @param before            true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count             获取消息条数
     * @param withUser          只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @return 由于ipc大小限制，本接口获取到的消息列表可能不完整，并且超级群里面，可能返回为完全加载的消息，请使用异步获取
     */
    @Deprecated()
    public List<Message> getMessagesEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<Integer> contentTypes, long fromIndex, boolean before, int count, String withUser) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return null;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
            lines == null || lines.size() == 0 ||
            contentTypes == null || contentTypes.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines or contentType");
            return null;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        try {
            return mClient.getMessagesEx(intypes, convertIntegers(lines), convertIntegers(contentTypes), fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取某类会话消息
     *
     * @param conversationTypes 会话类型列表
     * @param lines             会话线路列表
     * @param messageStatus     过滤消息状态，如果全部消息请为空
     * @param fromIndex         消息起始id(messageId)
     * @param before            true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count             获取消息条数
     * @param withUser          只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @return 由于ipc大小限制，本接口获取到的消息列表可能不完整，并且超级群里面，可能返回为完全加载的消息，请使用异步获取
     */
    @Deprecated
    public List<Message> getMessagesEx2(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<MessageStatus> messageStatus, long fromIndex, boolean before, int count, String withUser) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return null;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
            lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines");
            return null;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        int[] status = new int[messageStatus.size()];
        for (int i = 0; i < messageStatus.size(); i++) {
            status[i] = messageStatus.get(i).ordinal();
        }

        try {
            return mClient.getMessagesEx2(intypes, convertIntegers(lines), status, fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取会话消息
     *
     * @param conversation
     * @param fromIndex    消息起始id(messageId)
     * @param before       true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count        获取消息条数
     * @param withUser     只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @param callback     消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void getMessages(Conversation conversation, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {

        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            List<Message> outMsgs = new ArrayList<>();
            mClient.getMessagesAsync(conversation, fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    outMsgs.addAll(messages);
                    if (!hasMore) {
                        mainHandler.post(() -> callback.onSuccess(outMsgs, false));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 获取会话提醒消息
     *
     * @param conversation
     * @param fromIndex    消息起始id(messageId)
     * @param before       true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count        获取消息条数
     * @param callback     消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void getMentionedMessages(Conversation conversation, long fromIndex, boolean before, int count, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            List<Message> outMsgs = new ArrayList<>();
            mClient.getMentionedMessagesAsync(conversation, fromIndex, before, count, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    outMsgs.addAll(messages);
                    if (!hasMore) {
                        mainHandler.post(() -> callback.onSuccess(outMsgs, false));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 获取会话消息
     *
     * @param conversation 会话
     * @param contentTypes 消息类型列表
     * @param fromIndex    消息起始id(messageId)
     * @param before       true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count        获取消息条数
     * @param withUser     只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @param callback     消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void getMessages(Conversation conversation, List<Integer> contentTypes, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            List<Message> outMsgs = new ArrayList<>();
            mClient.getMessagesInTypesAsync(conversation, convertIntegers(contentTypes), fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    outMsgs.addAll(messages);
                    if (!hasMore) {
                        mainHandler.post(() -> callback.onSuccess(outMsgs, false));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 根据消息状态获取会话消息
     *
     * @param conversation  会话
     * @param messageStatus 消息状态列表
     * @param fromIndex     消息起始id(messageId)
     * @param before        true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count         获取消息条数
     * @param withUser      只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     */
    @Deprecated
    public List<Message> getMessagesByMessageStatus(Conversation conversation, List<Integer> messageStatus, long fromIndex, boolean before, int count, String withUser) {
        try {
            return mClient.getMessagesInStatusSync(conversation, convertIntegers(messageStatus), fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据消息状态获取会话消息
     *
     * @param conversation  会话
     * @param messageStatus 消息状态列表
     * @param fromIndex     消息起始id(messageId)
     * @param before        true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count         获取消息条数
     * @param withUser      只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @param callback      消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void getMessagesByMessageStatus(Conversation conversation, List<Integer> messageStatus, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getMessagesInStatusAsync(conversation, convertIntegers(messageStatus), fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 获取消息
     *
     * @param conversationTypes 会话类型
     * @param lines             会话线路
     * @param contentTypes      消息类型
     * @param fromIndex         消息起始id(messageId)
     * @param before            true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count             获取消息条数
     * @param withUser          只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @param callback          消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void getMessagesEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<Integer> contentTypes, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
            lines == null || lines.size() == 0 ||
            contentTypes == null || contentTypes.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines or contentType");
            callback.onFail(ErrorCode.INVALID_PARAMETER);
            return;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        try {
            mClient.getMessagesExAsync(intypes, convertIntegers(lines), convertIntegers(contentTypes), fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));

                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 获取消息
     *
     * @param conversationTypes 会话类型
     * @param lines             会话线路
     * @param messageStatus     消息状态
     * @param fromIndex         消息起始id(messageId)
     * @param before            true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count             获取消息条数
     * @param withUser          只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @param callback          消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void getMessagesEx2(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<MessageStatus> messageStatus, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }

        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
            lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines");
            callback.onFail(ErrorCode.INVALID_PARAMETER);
            return;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        int[] status = new int[messageStatus.size()];
        for (int i = 0; i < messageStatus.size(); i++) {
            status[i] = messageStatus.get(i).ordinal();
        }

        try {
            mClient.getMessagesEx2Async(intypes, convertIntegers(lines), status, fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 按照时间戳获取会话消息
     *
     * @param conversation 会话
     * @param contentTypes 消息类型列表
     * @param timestamp    时间戳
     * @param before       true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count        获取消息条数
     * @param withUser     只有会话类型为{@link Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @param callback     消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void getMessagesByTimestamp(Conversation conversation, List<Integer> contentTypes, long timestamp, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getMessagesInTypesAndTimestampAsync(conversation, convertIntegers(contentTypes), timestamp, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 获取用户在会话中的消息，比如获取某个用户在里面发送的消息列表
     *
     * @param userId       userId
     * @param conversation 会话
     * @param fromIndex    消息起始id(messageId)
     * @param before       true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count        获取消息条数
     * @param callback     消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void getUserMessages(String userId, Conversation conversation, long fromIndex, boolean before, int count, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getUserMessages(userId, conversation, fromIndex, before, count, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 获取消息
     *
     * @param userId            userId
     * @param conversationTypes 会话类型
     * @param lines             会话线路
     * @param contentTypes      消息类型
     * @param fromIndex         消息起始id(messageId)
     * @param before            true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count             获取消息条数
     * @param callback          消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void getUserMessagesEx(String userId, List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<Integer> contentTypes, long fromIndex, boolean before, int count, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
            lines == null || lines.size() == 0 ||
            contentTypes == null || contentTypes.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines or contentType");
            callback.onFail(ErrorCode.INVALID_PARAMETER);
            return;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        try {
            mClient.getUserMessagesEx(userId, intypes, convertIntegers(lines), convertIntegers(contentTypes), fromIndex, before, count, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));

                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 获取每天的消息数量。一般用于日历查看消息，在日历上显示每天是否有消息。
     * 后续如果查看某天的消息内容，可以计算出这天的开始和结束时间，然后使用根据时间获取消息的接口来加载消息。
     * <p>
     * 使用示例：
     * YearMonth yearMonth = YearMonth.of(2025, 7);
     * long startTime = LocalDateTime.of(yearMonth.atDay(1), LocalTime.MIN).atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
     * long endTime = LocalDateTime.of(yearMonth.atEndOfMonth(), LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
     * Map<String, Integer> dayCouts = getMessageCountByDay(conversation, null, startTime, endTime);
     * Log.d("ddd", dayCouts.toString());
     * <p>
     * 如果获取某天的消息，可以用 getMessages 方法，其中from可以用当天时间的毫秒数（也可用消息ID）。如果获取这天的消息，以降序排列，from就是这天的最后一秒毫秒数，然后count为正值。如果以升序排列，from为这一天的第一秒，count值为负值。
     *
     * @param conversation 会话
     * @param contentTypes 消息类型
     * @param startTime    开始时间，单位秒
     * @param endTime      结束时间，单位秒
     * @return 每天的消息数量。
     */
    public Map<String, Integer> getMessageCountByDay(Conversation conversation, List<Integer> contentTypes, long startTime, long endTime) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMessageCountByDay(conversation, convertIntegers(contentTypes), startTime, endTime);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取远程历史消息
     *
     * @param conversation     会话
     * @param contentTypes     拉取指定的类型。空时拉取所有消息。
     * @param beforeMessageUid 起始消息的消息uid
     * @param count            获取消息的条数
     * @param callback
     * @discussion 获取得到的消息数目有可能少于指定的count数，如果count不为0就意味着还有更多的消息可以获取，只有获取到的消息数为0才表示没有更多的消息了。
     */
    public void getRemoteMessages(Conversation conversation, List<Integer> contentTypes, long beforeMessageUid, int count, GetRemoteMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int[] intypes = null;
            if (contentTypes != null && !contentTypes.isEmpty()) {
                intypes = new int[contentTypes.size()];
                for (int i = 0; i < contentTypes.size(); i++) {
                    intypes[i] = contentTypes.get(i);
                }
            }
            List<Message> outMsgs = new ArrayList<>();
            mClient.getRemoteMessages(conversation, intypes, beforeMessageUid, count, new IGetRemoteMessagesCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    if (callback != null) {
                        outMsgs.addAll(messages);
                        if (!hasMore) {
                            mainHandler.post(() -> callback.onSuccess(outMsgs));
                        }
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取远程被引用的消息，消息会被消息变化通知。
     *
     * @param message 包含引用内容的文本消息
     */
    public void loadRemoteQuotedMessage(Message message) {
        if (!(message.content instanceof TextMessageContent)) {
            return;
        }
        TextMessageContent textMessageContent = (TextMessageContent) message.content;
        QuoteInfo quoteInfo = textMessageContent.getQuoteInfo();
        if (quoteInfo == null || quoteInfo.getMessageUid() == 0) {
            return;
        }

        this.getRemoteMessage(quoteInfo.getMessageUid(), new GetOneRemoteMessageCallback() {
            @Override
            public void onSuccess(Message msg) {
                textMessageContent.getQuoteInfo().setMessage(msg);
                for (OnMessageUpdateListener listener : messageUpdateListeners) {
                    listener.onMessageUpdate(message);
                }
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    /**
     * 获取远程历史消息
     *
     * @param messageUid 消息uid
     * @param callback
     */
    public void getRemoteMessage(long messageUid, GetOneRemoteMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.getRemoteMessage(messageUid, new IGetRemoteMessagesCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    if (callback != null) {
                        if (!hasMore) {
                            mainHandler.post(() -> {
                                callback.onSuccess(messages.get(0));
                            });
                        }
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取服务器消息，可以获取对应messageUid之前或者之后的消息。**如果消息是不连续的，saveToDb要为false，避免存储在本地，需要特别注意这一点。**
     *
     * @param conversation     会话
     * @param conversation 会话
     * @param messageUid 起始消息的ID
     * @param count 总数
     * @param before 是获取对应messageUid之前或者时候的消息
     * @param saveToDb 是否保存到本地DB中，如果是不连续的消息，千万要为false。
     * @param contentTypes 指定获取的类型。
     * @discussion 获取得到的消息数目有可能少于指定的count数，如果count不为0就意味着还有更多的消息可以获取，只有获取到的消息数为0才表示没有更多的消息了。
     */
    public void getRemoteMessages(Conversation conversation, List<Integer> contentTypes, long messageUid, int count, boolean before, boolean saveToDb, GetRemoteMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int[] intypes = null;
            if (contentTypes != null && !contentTypes.isEmpty()) {
                intypes = new int[contentTypes.size()];
                for (int i = 0; i < contentTypes.size(); i++) {
                    intypes[i] = contentTypes.get(i);
                }
            }
            List<Message> outMsgs = new ArrayList<>();
            mClient.getRemoteMessagesEx(conversation, intypes, messageUid, count, before, saveToDb, new IGetRemoteMessagesCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    if (callback != null) {
                        outMsgs.addAll(messages);
                        if (!hasMore) {
                            mainHandler.post(() -> callback.onSuccess(outMsgs));
                        }
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取远程文件记录
     *
     * @param conversation    会话，如果为空则获取当前用户所有收到和发出的文件记录
     * @param fromUser        文件发送用户，如果为空则获取该用户发出的文件记录
     * @param beforeMessageId 起始消息的消息id
     * @param order           文件记录排序
     * @param count           获取消息的条数
     * @param callback
     */
    public void getConversationFileRecords(Conversation conversation, String fromUser, long beforeMessageId, FileRecordOrder order, int count, GetFileRecordCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int intOrder = 0;
            if (order != null) {
                intOrder = order.value;
            }
            mClient.getConversationFileRecords(conversation, fromUser, beforeMessageId, intOrder, count, new IGetFileRecordCallback.Stub() {
                @Override
                public void onSuccess(List<FileRecord> messages) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess(messages);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取我的远程文件记录
     *
     * @param beforeMessageId 起始消息的消息id
     * @param order           文件记录排序
     * @param count           获取消息的条数
     * @param callback
     */
    public void getMyFileRecords(long beforeMessageId, FileRecordOrder order, int count, GetFileRecordCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int intOrder = 0;
            if (order != null) {
                intOrder = order.value;
            }
            mClient.getMyFileRecords(beforeMessageId, intOrder, count, new IGetFileRecordCallback.Stub() {
                @Override
                public void onSuccess(List<FileRecord> messages) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess(messages);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除远程文件记录，只能删除自己发送的。
     *
     * @param messageUid 文件对应的消息Uid
     * @param callback
     */
    public void deleteFileRecord(long messageUid, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.deleteFileRecord(messageUid, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 搜索我的远程文件记录
     *
     * @param keyword         关键字
     * @param beforeMessageId 起始消息的消息id
     * @param order           文件记录排序
     * @param count           获取消息的条数
     * @param callback
     */
    public void searchMyFileRecords(String keyword, long beforeMessageId, FileRecordOrder order, int count, GetFileRecordCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int intOrder = 0;
            if (order != null) {
                intOrder = order.value;
            }
            mClient.searchMyFileRecords(keyword, beforeMessageId, intOrder, count, new IGetFileRecordCallback.Stub() {
                @Override
                public void onSuccess(List<FileRecord> messages) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess(messages);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 搜索远程文件记录
     *
     * @param keyword         关键字
     * @param conversation    会话，如果为空则获取当前用户所有收到和发出的文件记录
     * @param fromUser        文件发送用户，如果为空则获取该用户发出的文件记录
     * @param beforeMessageId 起始消息的消息id
     * @param count           获取消息的条数
     * @param callback
     */
    public void searchFileRecords(String keyword, Conversation conversation, String fromUser, long beforeMessageId, FileRecordOrder order, int count, GetFileRecordCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int intOrder = 0;
            if (order != null) {
                intOrder = order.value;
            }
            mClient.searchFileRecords(keyword, conversation, fromUser, beforeMessageId, intOrder, count, new IGetFileRecordCallback.Stub() {
                @Override
                public void onSuccess(List<FileRecord> messages) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess(messages);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据消息id，获取消息
     *
     * @param messageId 消息id
     *                  <p>
     *                  消息uid。消息uid和消息id的区别是，每条消息都有uid，该uid由服务端生成，全局唯一；id是本地生成，
     *                  且和消息的存储类型{@link PersistFlag}相关，只有存储类型为
     *                  {@link PersistFlag#Persist_And_Count}
     *                  和{@link PersistFlag#Persist}的消息，有消息id
     * @return
     */
    public Message getMessage(long messageId) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMessage(messageId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据消息uid，获取消息
     *
     * @param messageUid 消息uid。
     *                   <p>
     *                   消息uid和消息id的区别是，每条消息都有uid，该uid由服务端生成，全局唯一；id是本地生成，
     *                   <p>
     *                   切和消息的存储类型{@link PersistFlag}相关，只有存储类型为
     *                   {@link PersistFlag#Persist_And_Count}
     *                   和{@link PersistFlag#Persist}的消息，有消息id
     * @return
     */
    public Message getMessageByUid(long messageUid) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMessageByUid(messageUid);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取会话的未读情况
     *
     * @param conversation
     * @return
     */
    public UnreadCount getUnreadCount(Conversation conversation) {
        if (!checkRemoteService()) {
            return new UnreadCount();
        }

        try {
            return mClient.getUnreadCount(conversation.type.ordinal(), conversation.target, conversation.line);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new UnreadCount();
    }

    /**
     * 获取指定会话类型和会话线路的未读情况
     *
     * @param conversationTypes
     * @param lines
     * @return
     */
    public UnreadCount getUnreadCountEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        if (!checkRemoteService()) {
            return new UnreadCount();
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            return mClient.getUnreadCountEx(intypes, inlines);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new UnreadCount();
    }


    /**
     * 清除指定会话的未读状态
     *
     * @param conversation
     */
    public boolean clearUnreadStatus(Conversation conversation) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            if (mClient.clearUnreadStatus(conversation.type.getValue(), conversation.target, conversation.line)) {
                ConversationInfo conversationInfo = getConversation(conversation);
                conversationInfo.unreadCount = new UnreadCount();
                for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                    listener.onConversationUnreadStatusClear(conversationInfo);
                }
                return true;
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 清除某些类会话的未读状态
     *
     * @param conversationTypes 会话类型列表
     * @param conversationTypes 会话线路列表
     */
    public boolean clearUnreadStatusEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        if (!checkRemoteService()) {
            return false;
        }
        int[] inTypes = new int[conversationTypes.size()];
        int[] inLines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            inTypes[i] = conversationTypes.get(i).ordinal();
        }
        for (int j = 0; j < lines.size(); j++) {
            inLines[j] = lines.get(j);
        }

        try {
            boolean result = mClient.clearUnreadStatusEx(inTypes, inLines);
            if (result) {
                List<ConversationInfo> conversationInfos = mClient.getConversationList(inTypes, inLines, false);
                for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                    for (ConversationInfo info : conversationInfos) {
                        listener.onConversationUnreadStatusClear(info);
                    }
                }
                return true;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 标记会话的为未读状态
     *
     * @param conversation      会话
     * @param syncToOtherClient 是否同步到其他端
     * @discussion 仅当会话中存在接收到的消息时，才能标记。
     */
    public boolean markAsUnRead(Conversation conversation, boolean syncToOtherClient) {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            boolean result = mClient.markAsUnRead(conversation.type.getValue(), conversation.target, conversation.line, syncToOtherClient);
            if (result) {
                ConversationInfo conversationInfo = getConversation(conversation);
                for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                    listener.onConversationUnreadStatusClear(conversationInfo);
                }
            }
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 清除掉一条消息的未读状态
     *
     * @param messageId 消息ID
     */
    public boolean clearMessageUnreadStatus(long messageId) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            Message msg = getMessage(messageId);
            if (msg != null) {
                if (mClient.clearMessageUnreadStatus(messageId)) {
                    ConversationInfo conversationInfo = getConversation(msg.conversation);
                    for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                        listener.onConversationUnreadStatusClear(conversationInfo);
                    }
                    return true;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 清除messageId之前（包含）的消息未读状态
     *
     * @param messageId
     * @param conversation
     */
    public boolean clearUnreadStatusBeforeMessage(long messageId, Conversation conversation) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            Message msg = getMessage(messageId);
            if (msg != null) {
                if (mClient.clearUnreadStatusBeforeMessage(messageId, conversation)) {
                    ConversationInfo conversationInfo = getConversation(msg.conversation);
                    if (conversationInfo != null) {
                        UnreadCount unreadCount = conversationInfo.unreadCount;
                        if (unreadCount.unread == 0 && unreadCount.unreadMention == 0 && unreadCount.unreadMentionAll == 0) {
                            for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                                listener.onConversationUnreadStatusClear(conversationInfo);
                            }
                        }
                    }
                    return true;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置audio获取media等媒体消息的播放状态(其实是可以设置所有类型的消息为已播放，可以根据业务需求来处理)
     *
     * @param messageId
     */
    public void setMediaMessagePlayed(long messageId) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setMediaMessagePlayed(messageId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置消息本地扩展信息
     *
     * @param messageId 消息ID
     * @param extra     附加信息
     * @return true更新成功，false更新失败
     */
    public boolean setMessageLocalExtra(long messageId, String extra) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            mClient.setMessageLocalExtra(messageId, extra);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 清除所有会话的未读状态
     * <p>
     * 已废弃，本方法不能将已读状态同步到服务端，卸载重装之后，会变回未读
     * <p>
     * 请使用{@link #clearUnreadStatus(Conversation)} 或 {@link #clearUnreadStatusEx(List, List)}
     */
    @Deprecated
    public void clearAllUnreadStatus() {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.clearAllUnreadStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除会话消息
     *
     * @param conversation
     */
    public void clearMessages(Conversation conversation) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.clearMessages(conversation.type.getValue(), conversation.target, conversation.line);

            for (OnClearMessageListener listener : clearMessageListeners) {
                listener.onClearMessage(conversation);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除会话消息，清空指定时间之前的所有消息
     *
     * @param conversation
     * @param beforeTime
     */
    public void clearMessages(Conversation conversation, long beforeTime) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int convType = 0;
            String target = "";
            int line = 0;
            if (conversation != null) {
                convType = conversation.type.getValue();
                target = conversation.target;
                line = conversation.line;
            }
            mClient.clearMessagesEx(convType, target, line, beforeTime);

            for (OnClearMessageListener listener : clearMessageListeners) {
                listener.onClearMessage(conversation);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除会话消息保留指定的最新条数消息
     *
     * @param conversation
     * @param keepCount
     */
    public void clearMessagesKeepLatest(Conversation conversation, int keepCount) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int convType = 0;
            String target = "";
            int line = 0;
            if (conversation != null) {
                convType = conversation.type.getValue();
                target = conversation.target;
                line = conversation.line;
            }
            mClient.clearMessagesEx2(convType, target, line, keepCount);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除所有会话
     *
     * @param removeConversation 是否同时删除会话信息.
     */
    public void clearAllMessages(boolean removeConversation) {
        if (!checkRemoteService()) {
            return;
        }

        try {

            mClient.clearAllMessages(removeConversation);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除会话
     *
     * @param conversation
     * @param clearMsg     是否同时删除该会话的所有消息
     */
    public void removeConversation(Conversation conversation, boolean clearMsg) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.removeConversation(conversation.type.ordinal(), conversation.target, conversation.line, clearMsg);
            for (OnRemoveConversationListener listener : removeConversationListeners) {
                listener.onConversationRemove(conversation);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 远程删除会话内所有消息，仅专业版IM服务支持。
     *
     * @param conversation 会话
     * @param callback     回调
     */
    public void clearRemoteConversationMessage(Conversation conversation, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.clearRemoteConversationMessage(conversation, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) mainHandler.post(() -> callback.onSuccess());
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_DIED));
        }
    }

    /**
     * 设置会话置顶
     *
     * @param conversation
     * @param top
     */
    public void setConversationTop(Conversation conversation, int top) {
        setConversationTop(conversation, top, null);
    }

    /**
     * 会话置顶
     *
     * @param conversation
     * @param top          true，置顶；false，取消置顶
     */
    public void setConversationTop(Conversation conversation, int top, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.setConversationTop(conversation.type.ordinal(), conversation.target, conversation.line, top, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    ConversationInfo conversationInfo = getConversation(conversation);
                    mainHandler.post(() -> {
                        for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                            listener.onConversationTopUpdate(conversationInfo, top);
                        }

                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * 设置会话草稿
     *
     * @param conversation
     * @param draft
     */
    public void setConversationDraft(Conversation conversation, @Nullable String draft) {
        if (conversation == null) {
            return;
        }

        if (!checkRemoteService()) {
            return;
        }

        try {
            ConversationInfo conversationInfo = getConversation(conversation);
            if (conversationInfo == null || (TextUtils.isEmpty(draft) && TextUtils.isEmpty(conversationInfo.draft)) || TextUtils.equals(draft, conversationInfo.draft)) {
                return;
            }
            mClient.setConversationDraft(conversation.type.ordinal(), conversation.target, conversation.line, draft);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        ConversationInfo conversationInfo = getConversation(conversation);
        for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
            listener.onConversationDraftUpdate(conversationInfo, draft);
        }
    }

    /**
     * 获取会话的已读情况
     *
     * @param conversation 会话，目前支持单聊和群聊
     * @return key-value, 分别表示userId和该用户已经读到了那个时间节点，可用该值和消息的server进行比较，比消息的serverTime大时，表示消息已读
     */
    public Map<String, Long> getConversationRead(Conversation conversation) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getConversationRead(conversation.type.getValue(), conversation.target, conversation.line);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取会话的送达情况
     *
     * @param conversation 会话，目前支持单聊和群聊
     * @return
     */
    public Map<String, Long> getMessageDelivery(Conversation conversation) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMessageDelivery(conversation.type.getValue(), conversation.target);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置会话时间戳, 如果会话不存在，会创建一个对应的会话
     *
     * @param conversation
     * @param timestamp
     */
    public void setConversationTimestamp(Conversation conversation, long timestamp) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setConversationTimestamp(conversation.type.ordinal(), conversation.target, conversation.line, timestamp);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 搜索用户
     *
     * @param keyword
     * @param searchUserType
     * @param page
     * @param callback
     */
    public void searchUser(String keyword, SearchUserType searchUserType, int page, final SearchUserCallback callback) {
        if (userSource != null) {
            userSource.searchUser(keyword, callback);
            return;
        }
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.searchUser(keyword, searchUserType.ordinal(), page, new cn.wildfirechat.client.ISearchUserCallback.Stub() {
                @Override
                public void onSuccess(final List<UserInfo> userInfos) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(userInfos);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 搜索用户
     *
     * @param domainId
     * @param keyword
     * @param searchUserType
     * @param page
     * @param callback
     */
    public void searchUserEx(String domainId, String keyword, SearchUserType searchUserType, int page, final SearchUserCallback callback) {
        if (userSource != null) {
            userSource.searchUser(keyword, callback);
            return;
        }
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.searchUserEx(domainId, keyword, searchUserType.ordinal(), page, new cn.wildfirechat.client.ISearchUserCallback.Stub() {
                @Override
                public void onSuccess(final List<UserInfo> userInfos) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(userInfos);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 搜索用户
     *
     * @param domainId
     * @param keyword
     * @param searchUserType
     * @param page
     * @param callback
     */
    public void searchUserEx2(String domainId, String keyword, SearchUserType searchUserType, UserSearchUserType userType, int page, final SearchUserCallback callback) {
        if (userSource != null) {
            userSource.searchUser(keyword, callback);
            return;
        }
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.searchUserEx2(domainId, keyword, searchUserType.ordinal(), userType.ordinal(), page, new cn.wildfirechat.client.ISearchUserCallback.Stub() {
                @Override
                public void onSuccess(final List<UserInfo> userInfos) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(userInfos);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 判断是否是好友关系
     *
     * @param userId
     * @return
     */
    public boolean isMyFriend(String userId) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.isMyFriend(userId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取好友id列表
     *
     * @param refresh 是否强制刷新好友列表，如果强制刷新好友列表，切好友列表有更新的话，会通过{@link OnFriendUpdateListener}回调通知
     * @return
     */
    public List<String> getMyFriendList(boolean refresh) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMyFriendList(refresh);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取好友列表
     *
     * @param refresh 是否强制刷新好友列表，如果强制刷新好友列表，切好友列表有更新的话，会通过{@link OnFriendUpdateListener}回调通知
     * @return
     */
    public List<Friend> getFriendList(boolean refresh) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getFriendList(refresh);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 优先级如下：
    // 1. 群备注 2. 好友备注 3. 用户displayName 4. <uid>
    public String getGroupMemberDisplayName(String groupId, String memberId) {
        UserInfo userInfo = getUserInfo(memberId, groupId, false);
        if (userInfo == null) {
            return "<" + memberId + ">";
        }
        if (!TextUtils.isEmpty(userInfo.groupAlias)) {
            return userInfo.groupAlias;
        } else if (WfcUtils.isExternalTarget(memberId)) {
            return this.getExternalUserDisplayName(memberId);
        } else if (!TextUtils.isEmpty(userInfo.friendAlias)) {
            return userInfo.friendAlias;
        } else if (!TextUtils.isEmpty(userInfo.displayName)) {
            return userInfo.displayName;
        }
        return "<" + memberId + ">";
    }

    public String getGroupMemberDisplayName(UserInfo userInfo) {
        if (!TextUtils.isEmpty(userInfo.groupAlias)) {
            return userInfo.groupAlias;
        } else if (WfcUtils.isExternalTarget(userInfo.uid)) {
            return this.getExternalUserDisplayName(userInfo.uid);
        } else if (!TextUtils.isEmpty(userInfo.friendAlias)) {
            return userInfo.friendAlias;
        } else if (!TextUtils.isEmpty(userInfo.displayName)) {
            return userInfo.displayName;
        }
        return "<" + userInfo.uid + ">";
    }

    public String getUserDisplayName(UserInfo userInfo) {
        if (userInfo == null) {
            return "";
        }
        if (WfcUtils.isExternalTarget(userInfo.uid)) {
            return this.getExternalUserDisplayName(userInfo.uid);
        } else if (!TextUtils.isEmpty(userInfo.friendAlias)) {
            return userInfo.friendAlias;
        } else if (!TextUtils.isEmpty(userInfo.displayName)) {
            return userInfo.displayName;
        }
        return "<" + userInfo.uid + ">";
    }

    public String getUserDisplayName(String userId) {
        UserInfo userInfo = getUserInfo(userId, false);
        return getUserDisplayName(userInfo);
    }

    public String getGroupDisplayName(String groupId) {
        GroupInfo groupInfo = getGroupInfo(groupId, false);
        return getGroupDisplayName(groupInfo);
    }

    public String getGroupDisplayName(GroupInfo groupInfo) {
        if (groupInfo == null) {
            return "群聊";
        }
        String name = !TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name;
        if (WfcUtils.isExternalTarget(groupInfo.target)) {
            String domainId = WfcUtils.getExternalDomainId(groupInfo.target);
            DomainInfo domainInfo = ChatManager.Instance().getDomainInfo(domainId, false);
            if (domainInfo != null) {
                name += " @" + domainInfo.name;
            }
        }
        return name;
    }

    /**
     * 获取好友别名
     *
     * @param userId
     * @return
     */
    public String getFriendAlias(String userId) {
        if (!checkRemoteService()) {
            return null;
        }
        String alias;
        try {
            alias = mClient.getFriendAlias(userId);
            return alias;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置好友别名
     *
     * @param userId
     * @param alias
     * @param callback
     */
    public void setFriendAlias(String userId, String alias, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setFriendAlias(userId, alias, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    userInfoCache.remove(userId);
                    if (callback != null) {
                        mainHandler.post(callback::onSuccess);
                    }

                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取好友附加信息
     *
     * @param userId
     * @return
     */
    public String getFriendExtra(String userId) {
        if (!checkRemoteService()) {
            return null;
        }
        String extra;
        try {
            extra = mClient.getFriendExtra(userId);
            return extra;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取好友列表
     *
     * @param refresh
     * @return
     */
    public List<UserInfo> getMyFriendListInfo(boolean refresh) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            List<String> userIds = mClient.getMyFriendList(refresh);
            if (userIds != null && !userIds.isEmpty()) {
                List<UserInfo> userInfos = new ArrayList<>();
                int step = isClientServiceInMainProcess ? Integer.MAX_VALUE : 400;
                int startIndex, endIndex;
                for (int i = 0; i <= userIds.size() / step; i++) {
                    startIndex = i * step;
                    endIndex = (i + 1) * step;
                    endIndex = Math.min(endIndex, userIds.size());
                    List<UserInfo> us = mClient.getUserInfos(userIds.subList(startIndex, endIndex), null);
                    userInfos.addAll(us);
                }
                if (userInfos.size() > 0) {
                    for (UserInfo info : userInfos) {
                        if (info != null) {
                            userInfoCache.put(info.uid, info);
                        }
                    }
                }
                return userInfos;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }


    /**
     * 异步获取好友列表
     *
     * @param refresh
     * @param callback
     */
    public void getMyFriendListInfoAsync(boolean refresh, GetUserInfoListCallback callback) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }
        internalWorkHandler.post(() -> {
            try {
                List<UserInfo> userInfos = new ArrayList<>();
                mClient.getFriendUserInfoListAsync(refresh, new IGetUserInfoListCallback.Stub() {
                    @Override
                    public void onSuccess(List<UserInfo> infos, boolean hasMore) throws RemoteException {
                        if (callback == null) {
                            return;
                        }
                        userInfos.addAll(infos);
                        if (!hasMore) {
                            mainHandler.post(() -> callback.onSuccess(userInfos));
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) throws RemoteException {
                        if (callback != null) {
                            mainHandler.post(() -> callback.onFail(errorCode));
                        }
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
                if (callback != null) {
                    mainHandler.post(() -> callback.onFail(-1));
                }
            }
        });
    }

    /**
     * 从服务端加载好友请求
     */
    public void loadFriendRequestFromRemote() {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.loadFriendRequestFromRemote();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取好友请求列表
     *
     * @param incoming true，收到的好友请求；false，发出的好友请求
     * @return
     */
    public List<FriendRequest> getFriendRequest(boolean incoming) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getFriendRequest(incoming);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取好友请求列表
     *
     * @param status 请求状态 RequestStatus_Sent/RequestStatus_Accepted/RequestStatus_Reject
     * @param incoming true，只包含收到的好友请求；false，所有好友请求
     * @return
     */
    public List<FriendRequest> getFriendRequestByStatus(int status, boolean incoming) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getFriendRequestByStatus(status, incoming);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取全部好友请求列表
     *
     * @return
     */
    public List<FriendRequest> getAllFriendRequest() {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getAllFriendRequest();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取好友请求
     *
     * @param userId   对方用户Id
     * @param incoming true，只包含收到的好友请求；false，所有好友请求
     * @return
     */
    public FriendRequest getFriendRequest(String userId, boolean incoming) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getOneFriendRequest(userId, incoming);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取好友请求数量
     *
     * @param status 请求状态 RequestStatus_Sent/RequestStatus_Accepted/RequestStatus_Reject
     * @param incoming true，只包含收到的好友请求；false，所有好友请求
     * @return
     */
    public int getFriendRequestCountByStatus(int status, boolean incoming) {
        if (!checkRemoteService()) {
            return 0;
        }

        try {
            return mClient.getFriendRequestCountByStatus(status, incoming);
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 清除好友请求未读状态
     */
    public void clearUnreadFriendRequestStatus() {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.clearUnreadFriendRequestStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取未读好友请求数
     *
     * @return
     */
    public int getUnreadFriendRequestStatus() {
        if (!checkRemoteService()) {
            return 0;
        }

        try {
            return mClient.getUnreadFriendRequestStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 删除好友请求
     *
     * @param direction  方向，true 是接收的，false 是发出的
     * @param beforeTime 删除某个时间之前的。如果删除所有用0.
     * @return 是否有数据被删除。
     */
    public boolean clearFriendRequest(boolean direction, long beforeTime) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.clearFriendRequest(direction, beforeTime);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除某个好友请求
     *
     * @param userId    用户ID
     * @param direction 方向，0是接收的，1是发出的
     * @return 是否有数据被删除。
     */
    public boolean deleteFriendRequest(String userId, boolean direction) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.deleteFriendRequest(userId, direction);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除好友
     *
     * @param userId
     * @param callback
     */
    public void removeFriend(String userId, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.removeFriend(userId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    /**
     * 发送好友请求
     *
     * @param userId
     * @param reason
     * @param callback
     */
    public void sendFriendRequest(String userId, String reason, String extra, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.sendFriendRequest(userId, reason, extra, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 处理好友请求
     *
     * @param userId
     * @param accept
     * @param extra    当接受好友请求时，extra会更新到好友的extra中，建议用json格式，为以后继续扩展保留空间
     * @param callback
     */
    public void handleFriendRequest(String userId, boolean accept, String extra, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.handleFriendRequest(userId, accept, extra, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 判断用户是否被加入了黑名单
     *
     * @param userId
     * @return
     */
    public boolean isBlackListed(String userId) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.isBlackListed(userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 获取黑名单列表
     *
     * @param refresh
     * @return
     */
    public List<String> getBlackList(boolean refresh) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getBlackList(refresh);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将用户加入或移除黑名单
     *
     * @param userId
     * @param isBlacked
     * @param callback
     */
    public void setBlackList(String userId, boolean isBlacked, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.setBlackList(userId, isBlacked, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 删除好友
     *
     * @param userId
     * @param callback
     */
    public void deleteFriend(String userId, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.deleteFriend(userId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }


    /**
     * 获取群信息
     *
     * @param groupId
     * @param refresh
     * @return
     * @discussion refresh 为true会导致一次网络同步，代价特别大，应该尽量避免使用true，仅当在进入此群会话中时使用一次true。
     */
    public @Nullable
    GroupInfo getGroupInfo(String groupId, boolean refresh) {
        if (!checkRemoteService()) {
            return new NullGroupInfo(groupId);
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.d(TAG, "get group info error, group id is empty");
            return null;
        }

        try {
            GroupInfo groupInfo = mClient.getGroupInfo(groupId, refresh);
            if (groupInfo == null) {
                groupInfo = new NullGroupInfo(groupId);
            }
            return groupInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取群信息
     *
     * @param groupId
     * @param refresh
     * @param callback
     * @discussion refresh 为true会导致一次网络同步，代价特别大，应该尽量避免使用true，仅当在进入此群会话中时使用一次true。
     */
    public void getGroupInfo(String groupId, boolean refresh, GetGroupInfoCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.getGroupInfoEx(groupId, refresh, new IGetGroupCallback.Stub() {
                @Override
                public void onSuccess(GroupInfo userInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(userInfo));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });

        } catch (RemoteException e) {
            e.printStackTrace();

        }
    }

    /**
     * 批量获取群信息
     *
     * @param groupIds
     * @param refresh
     * @return
     * @discussion refresh 为true会导致一次网络同步，代价特别大，应该尽量避免使用true，仅当在进入此群会话中时使用一次true。
     */
    public @Nullable
    List<GroupInfo> getGroupInfos(List<String> groupIds, boolean refresh) {
        List<GroupInfo> groupInfos = new ArrayList<>();
        if (!checkRemoteService()) {
            return groupInfos;
        }

        try {
            groupInfos = mClient.getGroupInfos(groupIds, refresh);
            if (groupInfos == null) {
                groupInfos = new ArrayList<>();
            }
            return groupInfos;
        } catch (RemoteException e) {
            e.printStackTrace();
            return groupInfos;
        }
    }

    /**
     * 加入聊天室
     *
     * @param chatRoomId
     * @param callback
     */
    public void joinChatRoom(String chatRoomId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(chatRoomId)) {
            Log.e(TAG, "Error, chatroomid is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.joinChatRoom(chatRoomId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onFail(errorCode);
                        }
                    });
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 退出聊天室
     *
     * @param chatRoomId
     * @param callback
     */
    public void quitChatRoom(String chatRoomId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.quitChatRoom(chatRoomId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 获取聊天室信息
     *
     * @param chatRoomId
     * @param updateDt
     * @param callback
     */
    public void getChatRoomInfo(String chatRoomId, long updateDt, GetChatRoomInfoCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.getChatRoomInfo(chatRoomId, updateDt, new cn.wildfirechat.client.IGetChatRoomInfoCallback.Stub() {
                @Override
                public void onSuccess(ChatRoomInfo chatRoomInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(chatRoomInfo));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 获取聊天室成员信息
     *
     * @param chatRoomId
     * @param maxCount   最多获取多少个成员信息
     * @param callback
     */
    public void getChatRoomMembersInfo(String chatRoomId, int maxCount, GetChatRoomMembersInfoCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }
        try {
            mClient.getChatRoomMembersInfo(chatRoomId, maxCount, new cn.wildfirechat.client.IGetChatRoomMembersInfoCallback.Stub() {
                @Override
                public void onSuccess(ChatRoomMembersInfo chatRoomMembersInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(chatRoomMembersInfo));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 获取已经加入聊天室的ID
     *
     * @return 已经加入聊天室的ID，如果为空或者空字符，说明未加入聊天室。
     */
    public String getJoinedChatroom() {
        if (!checkRemoteService()) {
            return null;
        }
        try {
            return mClient.getJoinedChatroom();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取用户信息
     *
     * @param userId
     * @param refresh
     * @return
     * @discussion refresh 为true会导致一次网络同步，代价特别大，应该尽量避免使用true，仅当在进入此人的单聊会话中时或者此人的用户信息页面使用一次true。
     */
    public UserInfo getUserInfo(String userId, boolean refresh) {
        return getUserInfo(userId, null, refresh);
    }

    /**
     * 当对应用户，本地不存在时，返回的{@link UserInfo}为{@link NullUserInfo}
     *
     * @param userId
     * @param groupId
     * @param refresh
     * @return
     * @discussion refresh 为true会导致一次网络同步，代价特别大，应该尽量避免使用true，仅当在进入此人的单聊会话中时或者此人的用户信息页面使用一次true。
     */
    public UserInfo getUserInfo(String userId, String groupId, boolean refresh) {
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "Error, user id is null");
            return null;
        }
        UserInfo userInfo = null;
        if (!refresh) {
            if (TextUtils.isEmpty(groupId)) {
                userInfo = userInfoCache.get(userId);
            }
            if (userInfo != null) {
                return userInfo;
            }
        }
        if (userSource != null) {
            userInfo = userSource.getUser(userId);
            if (userInfo == null) {
                userInfo = new NullUserInfo(userId);
            }
            return userInfo;
        }

        if (!checkRemoteService()) {
            return new NullUserInfo(userId);
        }

        try {
            userInfo = mClient.getUserInfo(userId, groupId, refresh);
            if (userInfo == null) {
                userInfo = new NullUserInfo(userId);
            } else {
                if (TextUtils.isEmpty(groupId)) {
                    userInfoCache.put(userId, userInfo);
                }
            }
            return userInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
            return new NullUserInfo(userId);
        }
    }

    /**
     * 批量获取用户信息，返回的list里面的元素可能为null
     *
     * @param userIds
     * @param groupId
     * @return
     */
    public List<UserInfo> getUserInfos(List<String> userIds, String groupId) {
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
        if (userSource != null) {
            List<UserInfo> userInfos = new ArrayList<>();
            for (String userId : userIds) {
                userInfos.add(userSource.getUser(userId));
            }
            return userInfos;
        }

        if (!checkRemoteService()) {
            return null;
        }

        try {
            List<UserInfo> userInfos = new ArrayList<>();
            int step = isClientServiceInMainProcess ? Integer.MAX_VALUE : 400;
            int startIndex, endIndex;
            for (int i = 0; i <= userIds.size() / step; i++) {
                startIndex = i * step;
                endIndex = (i + 1) * step;
                endIndex = Math.min(endIndex, userIds.size());
                List<UserInfo> us = mClient.getUserInfos(userIds.subList(startIndex, endIndex), groupId);
                userInfos.addAll(us);
            }
            if (userInfos.size() > 0) {
                for (UserInfo info : userInfos) {
                    if (info != null) {
                        if (TextUtils.isEmpty(groupId)) {
                            userInfoCache.put(info.uid, info);
                        }
                    }
                }
            }

            return userInfos;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 异步批量获取用户信息
     *
     * @param userIds
     * @param groupId
     * @param callback
     */
    public void getUserInfosAsync(List<String> userIds, String groupId, GetUserInfoListCallback callback) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

//        if (userSource != null) {
//            List<UserInfo> userInfos = new ArrayList<>();
//            for (String userId : userIds) {
//                userInfos.add(userSource.getUser(userId));
//            }
//            return userInfos;
//        }

        if (!checkRemoteService()) {
            callback.onFail(-1);
            return;
        }

        internalWorkHandler.post(() -> {
            try {
                List<UserInfo> userInfos = new ArrayList<>();
                mClient.getUserInfosAsync(userIds, groupId, new IGetUserInfoListCallback.Stub() {

                    @Override
                    public void onSuccess(List<UserInfo> infos, boolean hasMore) throws RemoteException {
                        userInfos.addAll(infos);
                        if (callback != null && !hasMore) {
                            mainHandler.post(() -> callback.onSuccess(userInfos));
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) throws RemoteException {
                        if (callback != null) {
                            mainHandler.post(() -> callback.onFail(errorCode));
                        }
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
                callback.onFail(-1);
            }
        });
    }

    /**
     * 异步获取用户信息
     *
     * @param userId
     * @param refresh
     * @param callback
     */
    public void getUserInfo(String userId, boolean refresh, GetUserInfoCallback callback) {
        getUserInfo(userId, null, refresh, callback);
    }

    /**
     * 异步获取用户信息。
     *
     * @param userId
     * @param groupId
     * @param refresh
     * @param callback
     */
    public void getUserInfo(String userId, String groupId, boolean refresh, GetUserInfoCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }
        try {
            mClient.getUserInfoEx(userId, groupId, refresh, new IGetUserCallback.Stub() {
                @Override
                public void onSuccess(UserInfo userInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(userInfo));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传媒体文件
     *
     * @param mediaPath
     * @param mediaType 媒体类型，可选值，参考{@link MessageContentMediaType}
     * @param callback
     */
    public void uploadMediaFile(String mediaPath, int mediaType, final UploadMediaCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.uploadMediaFile(mediaPath, mediaType, new IUploadMediaCallback.Stub() {
                @Override
                public void onSuccess(final String remoteUrl) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(remoteUrl);
                            }
                        });
                    }
                }

                @Override
                public void onProgress(final long uploaded, final long total) throws RemoteException {
                    callback.onProgress(uploaded, total);
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 上传媒体数据
     * 开始 TCP 短链接之后，该方法不可用，请使用{@link ChatManager#uploadMediaFile}
     *
     * @param data      不能超过1M，为了安全，实际只有900K
     * @param mediaType 媒体类型，可选值参考{@link MessageContentMediaType}
     * @param callback
     */
    public void uploadMedia(String fileName, byte[] data, int mediaType, final GeneralCallback2 callback) {
        uploadMedia2(fileName, data, mediaType, new UploadMediaCallback() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess(result);
            }

            @Override
            public void onProgress(long uploaded, long total) {

            }

            @Override
            public void onFail(int errorCode) {
                callback.onFail(errorCode);
            }
        });
    }

    /**
     * 上传媒体数据
     * 开始 TCP 短链接之后，该方法不可用，请使用{@link ChatManager#uploadMediaFile}
     *
     * @param data      不能超过1M，为了安全，实际只有900K
     * @param mediaType 媒体类型，可选值参考{@link MessageContentMediaType}
     * @param callback
     */
    public void uploadMedia2(String fileName, byte[] data, int mediaType, final UploadMediaCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (data.length > 900 * 1024) {
            if (callback != null) {
                callback.onFail(ErrorCode.FILE_TOO_LARGE);
            }
            return;
        }

        try {
            mClient.uploadMedia(fileName, data, mediaType, new IUploadMediaCallback.Stub() {
                @Override
                public void onSuccess(final String remoteUrl) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(remoteUrl);
                            }
                        });
                    }
                }

                @Override
                public void onProgress(final long uploaded, final long total) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onProgress(uploaded, total);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 修改个人信息
     *
     * @param values
     * @param callback
     */
    public void modifyMyInfo(List<ModifyMyInfoEntry> values, final GeneralCallback callback) {
        userInfoCache.remove(userId);
        if (userSource != null) {
            userSource.modifyMyInfo(values, callback);
            return;
        }
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.modifyMyInfo(values, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }

                    UserInfo userInfo = getUserInfo(userId, false);
                    onUserInfoUpdate(Collections.singletonList(userInfo));
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }

    }

    /**
     * 删除本地消息
     *
     * @param message
     * @return
     */
    public boolean deleteMessage(Message message) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            mClient.deleteMessage(message.messageId);
            for (OnDeleteMessageListener listener : deleteMessageListeners) {
                listener.onDeleteMessage(message);
            }
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 批量删除本地消息
     *
     * @param messageUids 消息的Uid列表
     * @return
     */
    public boolean batchDeleteMessages(List<Long> messageUids) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            long[] uids = new long[messageUids.size()];
            for (int i = 0; i < messageUids.size(); i++) {
                uids[i] = messageUids.get(i);
            }
            mClient.batchDeleteMessages(uids);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 清除指定用户的指定时间段发送的消息。
     *
     * @param userId    目标用户
     * @param startTime 开始时间，为0时忽略开始时间
     * @param endTime   结束时间，为0时忽略结束时间。
     * @return
     */
    public boolean clearUserMessage(String userId, long startTime, long endTime) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            mClient.clearUserMessage(userId, startTime, endTime);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除远程消息消息，只有专业版支持
     *
     * @param messageUid 消息的UID
     * @param callback   操作结果回调
     */
    public void deleteRemoteMessage(long messageUid, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.deleteRemoteMessage(messageUid, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> {
                        onDeleteMessage(messageUid);
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 更新远程消息消息内容，只有专业版支持。客户端仅能更新自己发送的消息，更新的消息类型不能变，更新的消息类型是服务配置允许更新的内容。Server API更新则没有限制。
     *
     * @param messageUid     消息的UID
     * @param messageContent 消息内容
     * @param distribute     是否分发给其他客户端
     * @param updateLocal    是否更新本地消息内容
     * @param callback       操作结果回调
     */
    public void updateRemoteMessageContent(long messageUid, MessageContent messageContent, boolean distribute, boolean updateLocal, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.updateRemoteMessageContent(messageUid, messageContent.encode(), distribute, updateLocal, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    Message updatedMessage = mClient.getMessageByUid(messageUid);
                    mainHandler.post(() -> {
                        for (OnMessageUpdateListener listener : messageUpdateListeners) {
                            listener.onMessageUpdate(updatedMessage);
                        }
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 搜索会话
     *
     * @param keyword
     * @param conversationTypes
     * @param lines
     * @return
     */
    public List<ConversationSearchResult> searchConversation(String keyword, List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        if (!checkRemoteService()) {
            return null;
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            return mClient.searchConversation(keyword, intypes, inlines);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 搜索会话
     *
     * @param keyword
     * @param conversationTypes
     * @param lines
     * @param startTime
     * @param endTime
     * @param desc
     * @param limit
     * @param offset
     * @return
     */
    public List<ConversationSearchResult> searchConversationEx(String keyword, List<Conversation.ConversationType> conversationTypes, List<Integer> lines, long startTime, long endTime, boolean desc, int limit, int offset) {
        if (!checkRemoteService()) {
            return null;
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            return mClient.searchConversationEx(keyword, intypes, inlines, startTime, endTime, desc, limit, offset);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 搜索某些类型会话中的消息。
     *
     * @param keyword
     * @param conversationTypes
     * @param lines
     * @param contentTypes
     * @param startTime
     * @param endTime
     * @param desc
     * @param limit
     * @param offset
     * @param onlyMentionedMsg
     * @return
     */
    public List<ConversationSearchResult> searchConversationEx2(String keyword, List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<Integer> contentTypes, long startTime, long endTime, boolean desc, int limit, int offset, boolean onlyMentionedMsg) {
        if (!checkRemoteService()) {
            return null;
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        int[] incnts = new int[contentTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        for (int k = 0; k < contentTypes.size(); k++) {
            incnts[k] = contentTypes.get(k);
        }

        try {
            return mClient.searchConversationEx2(keyword, intypes, inlines, incnts, startTime, endTime, desc, limit, offset, onlyMentionedMsg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 搜索消息
     *
     * @param conversation 会话为空时，搜索所有会话消息
     * @param keyword
     * @param desc
     * @param limit
     * @param offset
     * @param withUser
     * @return
     */
    public List<Message> searchMessage(Conversation conversation, String keyword, boolean desc, int limit, int offset, String withUser) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchMessage(conversation, keyword, desc, limit, offset, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 搜索提醒消息
     *
     * @param conversation 会话为空时，搜索所有会话消息
     * @param keyword
     * @param desc
     * @param limit
     * @param offset
     * @return
     */
    public List<Message> searchMentionedMessages(Conversation conversation, String keyword, boolean desc, int limit, int offset) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchMentionedMessages(conversation, keyword, desc, limit, offset);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 搜索消息
     *
     * @param conversation 会话为空时，搜索所有会话消息
     * @param keyword
     * @param contentTypes
     * @param desc
     * @param limit
     * @param offset
     * @return
     */
    public List<Message> searchMessageByTypes(Conversation conversation, String keyword, List<Integer> contentTypes, boolean desc, int limit, int offset, String withUser) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchMessageByTypes(conversation, keyword, convertIntegers(contentTypes), desc, limit, offset, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 搜索消息
     *
     * @param conversation 会话为空时，搜索所有会话消息
     * @param keyword
     * @param contentTypes
     * @param startTime
     * @param endTime
     * @param desc
     * @param limit
     * @param offset
     * @return
     */
    public List<Message> searchMessageByTypesAndTimes(Conversation conversation, String keyword, List<Integer> contentTypes, long startTime, long endTime, boolean desc, int limit, int offset, String withUser) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchMessageByTypesAndTimes(conversation, keyword, convertIntegers(contentTypes), startTime, endTime, desc, limit, offset, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 搜索消息
     *
     * @param conversationTypes 会话类型
     * @param lines             会话线路
     * @param contentTypes      消息类型
     * @param keyword           搜索关键字
     * @param fromIndex         消息起始id(messageId)
     * @param before            true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count             获取消息条数
     * @param callback          消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void searchMessagesEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<Integer> contentTypes, String keyword, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }
        if (conversationTypes == null || conversationTypes.size() == 0 ||
            lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines");
            return;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        try {
            mClient.searchMessagesEx(intypes, convertIntegers(lines), convertIntegers(contentTypes), keyword, fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 搜索提醒消息
     *
     * @param conversationTypes 会话类型
     * @param lines             会话线路
     * @param keyword           搜索关键字
     * @param desc              true, 获取fromIndex之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param limit             获取消息条数
     * @param offset            偏移数
     * @param callback          消息回调，当消息比较多，或者消息体比较大时，可能会回调多次
     */
    public void searchMentionedMessagesEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, String keyword, boolean desc, int limit, int offset, GetMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }
        if (conversationTypes == null || conversationTypes.size() == 0 ||
            lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines");
            return;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        try {
            mClient.searchMentionedMessagesEx(intypes, convertIntegers(lines), keyword, desc, limit, offset, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取签名。IM服务可以开启签名保护，只有指定签名的android客户端才可以登录
     *
     * @return
     * @throws PackageManager.NameNotFoundException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getSignature() throws PackageManager.NameNotFoundException, CertificateException, NoSuchAlgorithmException {
        String packageName = gContext.getPackageName();
        PackageInfo packageInfo = gContext.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
            Signature signature = packageInfo.signatures[0];
            byte[] signBytes = signature.toByteArray();

            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) fact.generateCertificate(new ByteArrayInputStream(signBytes));
            signBytes = certificate.getEncoded();

            signBytes = MessageDigest.getInstance("SHA1").digest(signBytes);
            String sign = Base64.getEncoder().encodeToString(signBytes);
            Log.d(TAG, "The app sign is " + sign);
            return sign;
        }
        return null;
    }

    /**
     * 搜索群组
     *
     * @param keyword
     * @return
     */
    public List<GroupSearchResult> searchGroups(String keyword) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchGroups(keyword);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 搜索好友，目前不支持拼音搜索，可以参考 {@link cn.wildfire.chat.kit.search.module.ContactSearchModule#search} 实现支持拼音搜索
     *
     * @param keyword
     * @return
     */
    public List<UserInfo> searchFriends(String keyword) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchFriends(keyword);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getEncodedClientId() {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getEncodedClientId();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 请求应用锁
     *
     * @param lockId
     * @param duration
     * @param callback
     */
    public void requireLock(String lockId, long duration, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.requireLock(lockId, duration, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 释放应用锁
     *
     * @param lockId
     * @param callback
     */
    public void releaseLock(String lockId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.releaseLock(lockId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 创建群组
     *
     * @param groupId
     * @param groupName
     * @param groupPortrait 已上传到文件存储的群头像的的链接地址
     * @param groupType
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void createGroup(String groupId, String groupName, String groupPortrait, GroupInfo.GroupType groupType, String groupExtra, List<String> memberIds, String memberExtra, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback2 callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.createGroup(groupId, groupName, groupPortrait, groupType.value(), groupExtra, memberIds, memberExtra, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback2.Stub() {
                @Override
                public void onSuccess(final String result) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(result);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 添加群成员
     *
     * @param groupId
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void addGroupMembers(String groupId, List<String> memberIds, String extra, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.addGroupMembers(groupId, memberIds, extra, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    private MessagePayload content2Payload(MessageContent content) {
        if (content == null) {
            return null;
        }
        MessagePayload payload = content.encode();
        payload.type = content.getClass().getAnnotation(ContentTag.class).type();
        return payload;
    }

    /**
     * 移除群成员
     *
     * @param groupId
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void removeGroupMembers(String groupId, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.removeGroupMembers(groupId, memberIds, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 退出群组
     *
     * @param groupId
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void quitGroup(String groupId, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        quitGroup(groupId, false, lines, notifyMsg, callback);
    }

    /**
     * 退出群组
     *
     * @param groupId
     * @param keepMessage 是否保留消息
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void quitGroup(String groupId, boolean keepMessage, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.quitGroup(groupId, keepMessage, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 解散群组
     *
     * @param groupId
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void dismissGroup(String groupId, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.dismissGroup(groupId, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 修改 群信息
     *
     * @param groupId
     * @param modifyType
     * @param newValue
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void modifyGroupInfo(String groupId, ModifyGroupInfoType modifyType, String newValue, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.modifyGroupInfo(groupId, modifyType.ordinal(), newValue, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    GroupInfo groupInfo = mClient.getGroupInfo(groupId, false);
                    onGroupInfoUpdated(Collections.singletonList(groupInfo));
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 修改我在群里面的别名
     *
     * @param groupId
     * @param alias
     * @param lines
     * @param notifyMsg
     * @param callback  回调成功只表示服务器修改成功了，本地可能还没更新。本地将服务器端的改动拉下来之后，会有{@link OnGroupMembersUpdateListener}通知回调
     */
    public void modifyGroupAlias(String groupId, String alias, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.modifyGroupAlias(groupId, alias, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                groupMemberCache.remove(groupMemberCacheKey(groupId, userId));
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 修改群成员在群中的别名，群主可以修改所有人的群别名，管理员可以修改普通成员的群别名
     *
     * @param groupId
     * @param memberId
     * @param alias
     * @param lines
     * @param notifyMsg
     * @param callback  回调成功只表示服务器修改成功了，本地可能还没更新。本地将服务器端的改动拉下来之后，会有{@link OnGroupMembersUpdateListener}通知回调
     */
    public void modifyGroupMemberAlias(String groupId, String memberId, String alias, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.modifyGroupMemberAlias(groupId, memberId, alias, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                groupMemberCache.remove(groupMemberCacheKey(groupId, userId));
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 修改群成员在群中的附加信息，群主可以修改所有人的，管理员可以修改普通成员的，所有人都可以修改自己的
     *
     * @param groupId
     * @param memberId
     * @param extra
     * @param lines
     * @param notifyMsg
     * @param callback  回调成功只表示服务器修改成功了，本地可能还没更新。本地将服务器端的改动拉下来之后，会有{@link OnGroupMembersUpdateListener}通知回调
     */
    public void modifyGroupMemberExtra(String groupId, String memberId, String extra, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.modifyGroupMemberExtra(groupId, memberId, extra, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                groupMemberCache.remove(groupMemberCacheKey(groupId, userId));
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 获取群成员列表，群成员特别多时，建议使用异步回调版本
     *
     * @param groupId
     * @param forceUpdate
     * @return
     * @discussion forceUpdate 为true会导致一次网络同步，代价特别大，应该尽量避免使用true，仅当在进入此群成员列表时使用一次true。
     */
    public List<GroupMember> getGroupMembers(String groupId, boolean forceUpdate) {
        if (!checkRemoteService()) {
            return null;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            return null;
        }
        try {
            return mClient.getGroupMembers(groupId, forceUpdate);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据类型来获取群成员
     *
     * @param groupId
     * @param type
     * @return
     */
    public List<GroupMember> getGroupMembersByType(String groupId, GroupMember.GroupMemberType type) {
        if (!checkRemoteService()) {
            return null;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "group id is null");
            return null;
        }

        try {
            return mClient.getGroupMembersByType(groupId, type.value());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取指定数量的群成员
     *
     * @param groupId
     * @param count
     * @return
     */
    public List<GroupMember> getGroupMembersByCount(String groupId, int count) {
        if (!checkRemoteService()) {
            return null;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "group id is null");
            return null;
        }

        try {
            return mClient.getGroupMembersByCount(groupId, count);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取群成员列表，支持超大群
     *
     * @param groupId
     * @param forceUpdate
     * @param callback
     * @discussion forceUpdate 为true会导致一次网络同步，代价特别大，应该尽量避免使用true，仅当在进入此群成员列表时使用一次true。
     */
    public void getGroupMembers(String groupId, boolean forceUpdate, GetGroupMembersCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        internalWorkHandler.post(() -> {
            try {
                List<GroupMember> groupMemberList = new ArrayList<>();
                mClient.getGroupMembersEx(groupId, forceUpdate, new IGetGroupMemberCallback.Stub() {
                    @Override
                    public void onSuccess(List<GroupMember> groupMembers, boolean hasMore) throws RemoteException {
                        groupMemberList.addAll(groupMembers);
                        if (!hasMore) {
                            if (callback != null) {
                                mainHandler.post(() -> callback.onSuccess(groupMemberList));
                            }
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) throws RemoteException {
                        if (callback != null) {
                            mainHandler.post(() -> callback.onFail(errorCode));
                        }
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onFail(-1);
                }
            }
        });
    }

    /**
     * 异步批量获取群成员用户信息
     *
     * @param groupId
     * @param refresh
     * @param callback
     */
    public void getGroupMemberUserInfosAsync(String groupId, boolean refresh, GetUserInfoListCallback callback) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }
        internalWorkHandler.post(() -> {
            try {
                List<UserInfo> userInfos = new ArrayList<>();
                mClient.getGroupMemberUserInfosAsync(groupId, refresh, new IGetUserInfoListCallback.Stub() {
                    @Override
                    public void onSuccess(List<UserInfo> infos, boolean hasMore) throws RemoteException {
                        if (callback == null) {
                            return;
                        }
                        userInfos.addAll(infos);
                        if (!hasMore) {
                            mainHandler.post(() -> callback.onSuccess(userInfos));
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) throws RemoteException {
                        if (callback != null) {
                            mainHandler.post(() -> callback.onFail(errorCode));
                        }
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
                if (callback != null) {
                    mainHandler.post(() -> callback.onFail(-1));
                }
            }
        });
    }

    private String groupMemberCacheKey(String groupId, String memberId) {
        return memberId + "@" + groupId;
    }

    /**
     * 获取群成员信息
     *
     * @param groupId
     * @param memberId
     * @return
     */
    public GroupMember getGroupMember(String groupId, String memberId) {
        if (TextUtils.isEmpty(groupId) || TextUtils.isEmpty(memberId)) {
            return null;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            return null;
        }

        String key = groupMemberCacheKey(groupId, memberId);
        GroupMember groupMember = groupMemberCache.get(key);
        if (groupMember != null) {
            return groupMember;
        }

        if (!checkRemoteService()) {
            return null;
        }

        try {
            groupMember = mClient.getGroupMember(groupId, memberId);
            groupMemberCache.put(key, groupMember);
            return groupMember;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     分批获取群成员信息

     @param groupId 群ID
     @param types 群成员类型，nil为所有
     @param offset offset
     @param count 群成员类型个数
     @return 群成员信息列表
     */
    public List<GroupMember> getGroupMembers(String groupId, List<GroupMember.GroupMemberType> types, int offset, int count) {
        if (!checkRemoteService()) {
            return new ArrayList<>();
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "group id is null");
            return new ArrayList<>();
        }

        if(types == null) types = new ArrayList<>();
        int[] intypes = new int[types.size()];
        for (int i = 0; i < types.size(); i++) {
            intypes[i] = types.get(i).ordinal();
        }

        try {
            return mClient.getGroupMembersEx2(groupId, intypes, offset, count);
        } catch (RemoteException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     获取群成员数量

     @param groupId 群ID
     @param types 群成员类型，nil为所有
     @return 群成员的数量
     */
    public int getGroupMembersCount(String groupId, List<GroupMember.GroupMemberType> types) {
        if (!checkRemoteService()) {
            return 0;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "group id is null");
            return 0;
        }
        if(types == null) types = new ArrayList<>();
        int[] intypes = new int[types.size()];
        for (int i = 0; i < types.size(); i++) {
            intypes[i] = types.get(i).ordinal();
        }

        try {
            return mClient.getGroupMembersCount(groupId, intypes);
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0;
        }
    }


    /**
     获取群成员ID

     @param groupId 群ID
     @param types 群成员类型，nil为所有
     @return 群成员ID列表
     */
    public List<String> getGroupMemberIds(String groupId, List<GroupMember.GroupMemberType> types) {
        if (!checkRemoteService()) {
            return new ArrayList<>();
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "group id is null");
            return new ArrayList<>();
        }
        if(types == null) types = new ArrayList<>();
        int[] intypes = new int[types.size()];
        for (int i = 0; i < types.size(); i++) {
            intypes[i] = types.get(i).ordinal();
        }

        try {
            return mClient.getGroupMemberIds(groupId, intypes);
        } catch (RemoteException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     从远程服务同步群组成员

     @param groupId 群ID
     */
    public void loadGroupMemberFromRemote(String groupId) {
        if (!checkRemoteService()) {
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "group id is null");
            return;
        }

        try {
            mClient.loadGroupMemberFromRemote(groupId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * 转让群组
     *
     * @param groupId
     * @param newOwner
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void transferGroup(String groupId, String newOwner, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.transferGroup(groupId, newOwner, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 设置群管理员
     *
     * @param groupId
     * @param isSet
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void setGroupManager(String groupId, boolean isSet, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.setGroupManager(groupId, isSet, memberIds, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 禁言群成员
     *
     * @param groupId
     * @param isSet
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void muteGroupMember(String groupId, boolean isSet, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.muteOrAllowGroupMember(groupId, isSet, memberIds, false, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 设置群成员允许名单，当设置群全局禁言时，仅群主/群管理/运行名单成员可以发言，仅专业版支持
     *
     * @param groupId
     * @param isSet
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void allowGroupMember(String groupId, boolean isSet, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.muteOrAllowGroupMember(groupId, isSet, memberIds, true, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 获取群备注
     *
     * @param groupId
     * @return
     */
    public String getGroupRemark(String groupId) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getGroupRemark(groupId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置群备注
     *
     * @param groupId
     * @param remark
     * @param callback
     */
    public void setGroupRemark(String groupId, String remark, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.setGroupRemark(groupId, remark, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    GroupInfo groupInfo = mClient.getGroupInfo(groupId, false);
                    onGroupInfoUpdated(Collections.singletonList(groupInfo));
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 获取当前用户所有群组ID，此方法消耗资源较大，不建议高频使用。
     *
     * @param callback
     */
    public void getMyGroups(final StringListCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getMyGroups(new cn.wildfirechat.client.IGeneralCallback3.Stub() {
                @Override
                public void onSuccess(List<String> results) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(results));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * 获取与指定用户同属群组的ID，此方法消耗资源较大，不建议高频使用。
     *
     * @param userId
     * @param callback
     */
    public void getCommonGroups(String userId, final StringListCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getCommonGroups(userId, new cn.wildfirechat.client.IGeneralCallback3.Stub() {
                @Override
                public void onSuccess(List<String> results) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(results));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    public byte[] encodeData(byte[] data) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.encodeData(data);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] decodeData(byte[] data) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.decodeData(data);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] decodeData(int type, byte[] data, boolean gzip) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.decodeDataEx(type, data, gzip);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void sendMomentsRequest(String path, byte[] mediaData, GeneralCallbackBytes callback) {
        if (!checkRemoteService()) {
            return;
        }
        internalWorkHandler.post(() -> {
            try {
                AshmenWrapper ashmenWrapper = AshmenWrapper.create(path, 1024 * 1024);
                ashmenWrapper.writeBytes(mediaData, 0, mediaData.length);
                mClient.sendMomentsRequest(path, ashmenWrapper, mediaData.length, new IGeneralCallbackInt.Stub() {
                    @Override
                    public void onSuccess(int length) throws RemoteException {
                        if (callback != null) {
                            byte[] data = new byte[length];
                            try {
                                ashmenWrapper.readBytes(data, 0, length);
                                callback.onSuccess(data);
                            } finally {
                                ashmenWrapper.close();
                            }
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) throws RemoteException {
                        if (callback != null) {
                            callback.onFail(errorCode);
                        }
                        ashmenWrapper.close();
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    // connect host
    public String getHost() {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getHost();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    // shortlink port
    public int getPort() {
        if (!checkRemoteService()) {
            return 80;
        }

        try {
            return mClient.getPort();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 80;
        }
    }

    // route host
    public String getHostEx() {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getHostEx();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取用户设置
     *
     * @param scope 相当于设置的命名空间，可选值参考{@link UserSettingScope}
     * @param key
     * @return
     */
    public String getUserSetting(int scope, String key) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getUserSetting(scope, key);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取用户设置
     *
     * @param scope 可选值参考{@link UserSettingScope}
     * @return
     */
    public Map<String, String> getUserSettings(int scope) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return (Map<String, String>) mClient.getUserSettings(scope);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取保存到通讯录的群组信息
     *
     * @param callback
     */
    public void getFavGroups(final GetGroupsCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        internalWorkHandler.post(() -> {
            Map<String, String> groupIdMap = getUserSettings(UserSettingScope.FavoriteGroup);
            List<GroupInfo> groups = new ArrayList<>();
            if (groupIdMap != null && !groupIdMap.isEmpty()) {
                for (Map.Entry<String, String> entry : groupIdMap.entrySet()) {
                    if (entry.getValue().equals("1")) {
                        GroupInfo info = getGroupInfo(entry.getKey(), false);
                        if (!(info instanceof NullGroupInfo)) {
                            groups.add(getGroupInfo(entry.getKey(), false));
                        }
                    }
                }
            }
            mainHandler.post(() -> callback.onSuccess(groups));
        });
    }

    /**
     * 是否是收藏群组
     *
     * @param groupId
     * @return
     */
    public boolean isFavGroup(String groupId) {
        if (!checkRemoteService()) {
            return false;
        }

        String value = getUserSetting(UserSettingScope.FavoriteGroup, groupId);
        if (value == null || !value.equals("1")) {
            return false;
        }
        return true;
    }

    /**
     * 设置收藏群组
     *
     * @param groupId
     * @param isSet
     * @param callback
     */
    public void setFavGroup(String groupId, boolean isSet, GeneralCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        setUserSetting(UserSettingScope.FavoriteGroup, groupId, isSet ? "1" : "0", callback);
    }

    /**
     * 获取星标用户
     *
     * @param callback
     */
    public void getFavUsers(final StringListCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        internalWorkHandler.post(() -> {
            Map<String, String> userIdMap = getUserSettings(UserSettingScope.FavoriteUser);
            List<String> userIds = new ArrayList<>();
            if (userIdMap != null && !userIdMap.isEmpty()) {
                for (Map.Entry<String, String> entry : userIdMap.entrySet()) {
                    if (entry.getValue().equals("1")) {
                        userIds.add(entry.getKey());
                    }
                }
            }
            mainHandler.post(() -> callback.onSuccess(userIds));
        });
    }

    /**
     * 是否是星标用户
     *
     * @param userId
     * @return
     */
    public boolean isFavUser(String userId) {
        if (!checkRemoteService()) {
            return false;
        }
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "Error, user id is null");
            return false;
        }

        String value = getUserSetting(UserSettingScope.FavoriteUser, userId);
        if (value == null || !value.equals("1")) {
            return false;
        }
        return true;
    }

    /**
     * 设置星标用户
     *
     * @param userId
     * @param isSet
     * @param callback
     */
    public void setFavUser(String userId, boolean isSet, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "Error, user id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        setUserSetting(UserSettingScope.FavoriteUser, userId, isSet ? "1" : "0", callback);
    }

    /*
    获取收藏群组，此方法已废弃，请使用 getFavGroups
     */
    @Deprecated
    public void getMyGroups(final GetGroupsCallback callback) {
        getFavGroups(callback);
    }

    /**
     * 获取自己的自定义状态。
     *
     * @return 返回自己的自定义状态。
     */
    public Pair<Integer, String> getMyCustomState() {
        try {
            String str = getUserSetting(UserSettingScope.CustomState, "");
            if (TextUtils.isEmpty(str) || !str.contains("-")) {
                return new Pair<>(0, null);
            }
            int index = str.indexOf("-");
            int state = Integer.parseInt(str.substring(0, index));
            String text = str.substring(index + 1);
            return new Pair<>(state, text);
        } catch (Exception e) {
            return new Pair<>(0, null);
        }
    }

    /**
     * 设置自己的自定义状态。
     *
     * @param state    自定义状态
     * @param text     自定义状态描述
     * @param callback 设置结果回调
     */
    public void setMyCustomState(int state, String text, GeneralCallback callback) {
        String str = state + "-";
        if (!TextUtils.isEmpty(text)) {
            str += text;
        }
        setUserSetting(UserSettingScope.CustomState, "", str, callback);
    }

    /**
     * 设置用户设置信息
     *
     * @param scope
     * @param key
     * @param value
     * @param callback
     */
    public void setUserSetting(int scope, String key, String value, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setUserSetting(scope, key, value, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                    onSettingUpdated();
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 会话免打扰
     *
     * @param conversation
     * @param silent
     */
    public void setConversationSilent(Conversation conversation, boolean silent) {
        setConversationSilent(conversation, silent, null);
    }

    /**
     * 会话免打扰
     *
     * @param conversation
     * @param silent
     * @param callback
     */
    public void setConversationSilent(Conversation conversation, boolean silent, GeneralCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setConversationSilent(conversation.type.ordinal(), conversation.target, conversation.line, silent, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> {
                        ConversationInfo conversationInfo = getConversation(conversation);
                        for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                            listener.onConversationSilentUpdate(conversationInfo, silent);
                        }
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取认证码。请参考野火开放平台服务
     *
     * @param appId
     * @param appType
     * @param host
     * @param callback
     */
    public void getAuthCode(String appId, int appType, String host, GeneralCallback2 callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getAuthCode(appId, appType, host, new IGeneralCallback2.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(success));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * config应用，请参考野火开放平台服务
     *
     * @param appId
     * @param appType
     * @param timestamp
     * @param nonceStr
     * @param signature
     * @param callback
     */
    public void configApplication(String appId, int appType, long timestamp, String nonceStr, String signature, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.configApplication(appId, appType, timestamp, nonceStr, signature, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess());
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return 服务器时间 - 本地时间
     */
    public long getServerDeltaTime() {
        if (!checkRemoteService()) {
            return 0L;
        }

        try {
            return mClient.getServerDeltaTime();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * @return 服务器时间 - 本地时间
     */
    public long getServerTimestamp() {
        long now = System.currentTimeMillis();
        long delta = this.getServerDeltaTime();
        return now + delta;
    }

    /**
     * 当前服务是否连接到了主网络
     */
    public boolean isConnectedToMainNetwork() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.isConnectedToMainNetwork();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getImageThumbPara() {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getImageThumbPara();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取会话消息数
     *
     * @param conversation
     * @return
     */
    public int getMessageCount(Conversation conversation) {
        if (!checkRemoteService()) {
            return 0;
        }

        try {
            return mClient.getMessageCount(conversation);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 根据会话类型获取消息条数
     *
     * @param conversationTypes 目标会话类型数组
     * @param lines             目标会话线路数组
     * @return 消息条数
     */
    public int getConversationMessageCount(int[] conversationTypes, int[] lines) {
        if (!checkRemoteService()) {
            return 0;
        }

        try {
            return mClient.getConversationMessageCount(conversationTypes, lines);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 设置代理，只支持socks5代理，只有专业版支持。
     *
     * @param proxyInfo 代理信息
     */
    public void setProxyInfo(Socks5ProxyInfo proxyInfo) {
        this.proxyInfo = proxyInfo;
        if (checkRemoteService()) {
            try {
                mClient.setProxyInfo(proxyInfo);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开启事务，数据库备份、批量插入数据时，可使用事务，那样效率更高。
     *
     * @return
     */
    public boolean beginTransaction() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.begainTransaction();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 提交事务
     */
    public boolean commitTransaction() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.commitTransaction();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 回滚事务
     */
    public boolean rollbackTransaction() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.rollbackTransaction();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 是否是专业版IM服务
     *
     * @return
     */
    public boolean isCommercialServer() {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            return mClient.isCommercialServer();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * IM服务是否开启了已读报告功能
     *
     * @return
     */
    public boolean isReceiptEnabled() {
        if (!checkRemoteService()) {
            return false;
        }
        if (receiptStatus != -1) {
            return receiptStatus == 1;
        }

        try {
            boolean isReceiptEnabled = mClient.isReceiptEnabled();
            receiptStatus = isReceiptEnabled ? 1 : 0;
            return isReceiptEnabled;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * IM服务群组是否开启了已读报告功能
     *
     * @return
     */
    public boolean isGroupReceiptEnabled() {
        if (!checkRemoteService()) {
            return false;
        }
        if (groupReceiptStatus != -1) {
            return groupReceiptStatus == 1;
        }

        try {
            boolean isGroupReceiptEnabled = mClient.isGroupReceiptEnabled();
            groupReceiptStatus = isGroupReceiptEnabled ? 1 : 0;
            return isGroupReceiptEnabled;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * IM服务是否支持密聊
     *
     * @return
     */
    public boolean isEnableSecretChat() {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            return mClient.isEnableSecretChat();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * IM服务是否开启了互联功能
     *
     * @return
     */
    public boolean isEnableMesh() {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            return mClient.isEnableMesh();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断当前用户是否开启密聊功能。开关仅影响密聊的创建，已经创建的密聊不受此开关影响。
     *
     * @return
     */
    public boolean isUserEnableSecretChat() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            boolean disable = "1".equals(mClient.getUserSetting(UserSettingScope.DisableSecretChat, ""));
            return !disable;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置当前用户是否开启密聊功能。开关仅影响密聊的创建，已经创建的密聊不受此开关影响。
     *
     * @param enable
     * @param callback
     */
    public void setUserEnableSecretChat(boolean enable, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.DisableSecretChat, "", enable ? "0" : "1", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess();
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 当前用户是否开启添加好友需要验证，默认为开启
     *
     * @return true，需要验证；false，不需要验证
     */
    public boolean isAddFriendNeedVerify() {
        String value = this.getUserSetting(UserSettingScope.AddFriend_NoVerify, "");
        return !"1".equals(value);
    }

    /**
     * 修改当前用户是否开启添加好友需要验证功能
     *
     * @param enable   是否需要验证
     * @param callback 结果回调
     */
    public void setAddFriendNeedVerify(boolean enable, GeneralCallback callback) {
        this.setUserSetting(UserSettingScope.AddFriend_NoVerify, "", enable ? "0" : "1", callback);
    }


    /**
     * IM服务进程是否bind成功
     *
     * @return
     */
    public boolean isIMServiceConnected() {
        return mClient != null;
    }

    /**
     * 开启协议栈日志
     */
    public void startLog() {
        Log.d(TAG, "startLog");
        startLog = true;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.startLog();
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 关闭协议栈日志
     */
    public void stopLog() {
        Log.d(TAG, "stopLog");
        startLog = false;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.stopLog();
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 设置发送日志命令
     *
     * @param sendLogCommand
     */
    public void setSendLogCommand(String sendLogCommand) {
        this.sendLogCommand = sendLogCommand;
    }

    /**
     * 获取协议栈版本
     *
     * @return 协议栈版本
     */
    public String getProtoRevision() {
        if (!checkRemoteService()) {
            return "";
        }

        try {
            return mClient.getProtoRevision();
        } catch (RemoteException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取数据目录所在磁盘的可用空间。
     *
     * @return 磁盘的可用空间
     */
    public long getDiskSpaceAvailableSize() {
        if (!checkRemoteService()) {
            return -1;
        }

        try {
            return mClient.getDiskSpaceAvailableSize();
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private String getLogPath() {
        return gContext.getCacheDir().getAbsolutePath() + "/log";
    }

    /**
     * 获取日志文件路径
     *
     * @return
     */
    public List<String> getLogFilesPath() {
        List<String> paths = new ArrayList<>();
        String path = getLogPath();

        //遍历path目录下的所有日志文件，以wflog开头的
        File dir = new File(path);
        File[] subFile = dir.listFiles();
        if (subFile != null) {
            for (File file : subFile) {
                //wflog为ChatService中定义的，如果修改需要对应修改
                if (file.isFile() && file.getName().startsWith("wflog_")) {
                    paths.add(file.getAbsolutePath());
                }
            }
        }

        String xlogMMapDir = gContext.getFilesDir().getAbsolutePath() + "/xlog";
        subFile = new File(xlogMMapDir).listFiles();
        if (subFile != null) {
            for (File file : subFile) {
                //wflog为ChatService中定义的，如果修改需要对应修改
                if (file.isFile() && file.getName().startsWith("wflog_")) {
                    paths.add(file.getAbsolutePath());
                }
            }
        }

        // sort paths
        Collections.sort(paths, (f1, f2) -> {
            String n1 = f1.substring(f1.lastIndexOf("/") + 1);
            String n2 = f2.substring(f2.lastIndexOf("/") + 1);
            return n1.compareTo(n2);
        });

        return paths;
    }

    /**
     * 设置第三方推送设备token
     *
     * @param token
     * @param pushType 使用什么推送你，可选值参考{@link cn.wildfirechat.push.PushService.PushServiceType}，这个值最大不能超过127
     */
    public void setDeviceToken(String token, int pushType) {
        Log.d(TAG, "setDeviceToken " + token + " " + pushType);
        deviceToken = token;
        this.pushType = pushType;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setDeviceToken(token, pushType);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 判断是否是是全局消息免打扰
     *
     * @return
     */
    public boolean isGlobalSilent() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return "1".equals(mClient.getUserSetting(UserSettingScope.GlobalSilent, ""));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置全局消息免打扰
     *
     * @param isSilent
     * @param callback
     */
    public void setGlobalSilent(boolean isSilent, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.GlobalSilent, "", isSilent ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否是是实时音视频通话免打扰
     *
     * @return
     */
    public boolean isVoipSilent() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return "1".equals(mClient.getUserSetting(UserSettingScope.VoipSilent, ""));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置实时音视频通话免打扰
     *
     * @param isSilent
     * @param callback
     */
    public void setVoipSilent(boolean isSilent, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.VoipSilent, "", isSilent ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否禁止同步草稿功能，仅专业版支持
     *
     * @return
     */
    public boolean isDisableSyncDraft() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return "1".equals(mClient.getUserSetting(UserSettingScope.DisableSyncDraft, ""));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置是否禁止同步草稿，仅专业版支持
     *
     * @param isEnable
     * @param callback
     */
    public void setDisableSyncDraft(boolean isEnable, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.DisableSyncDraft, "", isEnable ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 应用全局是否禁止同步草稿功能，仅专业版支持
     *
     * @return
     */
    public boolean isGlobalDisableSyncDraft() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.isGlobalDisableSyncDraft();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 当前时间是否免打扰
     *
     * @return
     */
    public boolean isNoDisturbing() {
        CountDownLatch count = new CountDownLatch(1);
        boolean[] results = {false};
        getNoDisturbingTimes((isNoDisturbing, startMins, endMins) -> {
            int nowMin = ((int) (System.currentTimeMillis() / 1000 / 60)) % (24 * 60);
            if (isNoDisturbing) {
                if (endMins > startMins) {
                    if (nowMin > startMins && nowMin < endMins) {
                        results[0] = true;
                    }
                } else {
                    if (nowMin > startMins || nowMin < endMins) {
                        results[0] = true;
                    }
                }
            }
            count.countDown();
        });
        try {
            count.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return results[0];
    }

    /**
     * 获取免打扰时间回调
     */
    public interface GetNoDisturbingTimesCallback {
        /**
         * 返回免打扰时间
         *
         * @param isNoDisturbing 是否开启了免打扰。
         * @param startMins      起始时间，UTC时间0点起的的分钟数，需要注意与本地时间转换。
         * @param endMins        结束时间，UTC时间0点起的的分钟数，需要注意与本地时间转换。如果小于起始时间表示是隔夜。
         */
        void onResult(boolean isNoDisturbing, int startMins, int endMins);
    }

    /**
     * 获取免打扰时间段
     *
     * @param callback 结果回调。
     */
    public void getNoDisturbingTimes(GetNoDisturbingTimesCallback callback) {
        if (!checkRemoteService()) {
            callback.onResult(false, 0, 0);
            return;
        }

        try {
            String value = mClient.getUserSetting(UserSettingScope.NoDisturbing, "");
            if (!TextUtils.isEmpty(value)) {
                String[] arrs = value.split("\\|");
                if (arrs.length == 2) {
                    int start = Integer.parseInt(arrs[0]);
                    int end = Integer.parseInt(arrs[1]);
                    callback.onResult(true, start, end);
                    return;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        callback.onResult(false, 0, 0);
    }

    /**
     * 设置免打扰时间段
     *
     * @param startMins 起始时间，UTC时间0点起的的分钟数，需要注意与本地时间转换。
     * @param endMins   结束时间，UTC时间0点起的的分钟数，需要注意与本地时间转换。如果小于起始时间表示是隔夜。
     * @param callback  处理结果回调
     */
    public void setNoDisturbingTimes(int startMins, int endMins, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.NoDisturbing, "", startMins + "|" + endMins, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消免打扰时间
     *
     * @param callback 处理结果回调
     */
    public void clearNoDisturbingTimes(final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.NoDisturbing, "", "", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否隐藏通知详情
     *
     * @return
     */
    public boolean isHiddenNotificationDetail() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return "1".equals(mClient.getUserSetting(UserSettingScope.HiddenNotificationDetail, ""));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置隐藏通知详情
     *
     * @param isHidden
     * @param callback
     */
    public void setHiddenNotificationDetail(boolean isHidden, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.HiddenNotificationDetail, "", isHidden ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否隐藏群组会话中群成员名字
     *
     * @param groupId
     * @return
     */
    public boolean isHiddenGroupMemberName(String groupId) {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            return "1".equals(mClient.getUserSetting(UserSettingScope.GroupHideNickname, groupId));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置是否隐藏群组会话中群成员名字
     *
     * @param groupId
     * @param hidden
     * @param callback
     */
    public void setHiddenGroupMemberName(String groupId, boolean hidden, GeneralCallback callback) {
        setUserSetting(UserSettingScope.GroupHideNickname, groupId, hidden ? "1" : "0", callback);
    }

    /**
     * 判断当前用户是否开启消息回执
     *
     * @return
     */
    public boolean isUserEnableReceipt() {
        if (!checkRemoteService()) {
            return false;
        }
        if (userReceiptStatus != -1) {
            return userReceiptStatus == 1;
        }

        try {
            boolean disable = "1".equals(mClient.getUserSetting(UserSettingScope.DisableReceipt, ""));
            userReceiptStatus = disable ? 0 : 1;
            return !disable;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置当前用户是否开启消息回执
     *
     * @param enable
     * @param callback
     */
    public void setUserEnableReceipt(boolean enable, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.DisableReceipt, "", enable ? "0" : "1", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            userReceiptStatus = enable ? 1 : 0;
                            callback.onSuccess();
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取授权访问的链接地址
     *
     * @param messageUid 消息的Uid
     * @param mediaType  媒体类型
     * @param mediaPath  媒体的路径
     * @param callback   返回经过授权的媒体地址
     */
    public void getAuthorizedMediaUrl(long messageUid, MessageContentMediaType mediaType, String mediaPath, final GetAuthorizedMediaUrlCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getAuthorizedMediaUrl(messageUid, mediaType.getValue(), mediaPath, new IGetAuthorizedMediaUrlCallback.Stub() {
                @Override
                public void onSuccess(String authorizedUrl, String backupUrl) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(authorizedUrl, backupUrl);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 是否支持大文件上传。只有专业版支持，当支持大文件上传时，使用getUploadUrl获取到上传链接，然后在应用层上传。
     *
     * @return
     */
    public boolean isSupportBigFilesUpload() {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            return mClient.isSupportBigFilesUpload();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取文件上传的链接地址，一般用在大文件上传，通过isSupportBigFilesUpload方法检查之后才可以使用。
     *
     * @param fileName    文件名
     * @param mediaType   媒体类型
     * @param contentType Http的ContentType Header，可以为空，为空时默认为"application/octet-stream"
     * @param callback    返回上传地址
     */
    public void getUploadUrl(String fileName, MessageContentMediaType mediaType, String contentType, GetUploadUrlCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getUploadUrl(fileName, mediaType.ordinal(), contentType, new IGetUploadUrlCallback.Stub() {
                @Override
                public void onSuccess(String uploadUrl, String remoteUrl, String backupUploadUrl, int type) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(uploadUrl, remoteUrl, backupUploadUrl, type));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }

    }

    /**
     * 获取PC端在线情况，包括PC端，Pad端，Web端和小程序端。
     *
     * @return
     */
    public List<PCOnlineInfo> getPCOnlineInfos() {
        String pcOnline = getUserSetting(UserSettingScope.PCOnline, "PC");
        String webOnline = getUserSetting(UserSettingScope.PCOnline, "Web");
        String wxOnline = getUserSetting(UserSettingScope.PCOnline, "WX");
        String padOnline = getUserSetting(UserSettingScope.PCOnline, "Pad");

        List<PCOnlineInfo> infos = new ArrayList<>();
        PCOnlineInfo info = PCOnlineInfo.infoFromStr(pcOnline, PCOnlineInfo.PCOnlineType.PC_Online);
        if (info != null) {
            infos.add(info);
        }
        info = PCOnlineInfo.infoFromStr(webOnline, PCOnlineInfo.PCOnlineType.Web_Online);
        if (info != null) {
            infos.add(info);
        }
        info = PCOnlineInfo.infoFromStr(wxOnline, PCOnlineInfo.PCOnlineType.WX_Online);
        if (info != null) {
            infos.add(info);
        }
        info = PCOnlineInfo.infoFromStr(padOnline, PCOnlineInfo.PCOnlineType.Pad_Online);
        if (info != null) {
            infos.add(info);
        }

        return infos;
    }

    /**
     * 踢掉PC端在线设备（包括PC端，Web端和小程序端)
     *
     * @param pcClientId 端的设备ID
     * @param callback   处理结果
     **/
    public void kickoffPCClient(String pcClientId, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.kickoffPCClient(pcClientId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(callback::onSuccess);
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否开启了PC在线时，移动端静音操作
     *
     * @return 当为true时，如果PC端（包括pc端，web端和小程序端）在线，移动端将不再收到提醒
     **/
    public boolean isMuteNotificationWhenPcOnline() {
        if (!checkRemoteService()) {
            return false;
        }

        String value = getUserSetting(UserSettingScope.MuteWhenPcOnline, "");
        if (value == null || !value.equals("1")) {
            return defaultSilentWhenPCOnline;
        }
        return !defaultSilentWhenPCOnline;
    }

    /**
     * 设置PC/Web在线时，手机是否默认静音。缺省值为YES，如果IM服务配置server.mobile_default_silent_when_pc_online 为false时，需要调用此函数设置为false，此时翻转静音的意义。
     *
     * @param defaultSilent 缺省值是否为静音。
     */
    public void setDefaultSilentWhenPcOnline(boolean defaultSilent) {
        defaultSilentWhenPCOnline = defaultSilent;
    }

    /**
     * 设置开启了PC在线时，移动端是否静音操作
     *
     * @param isMute
     * @param callback
     **/
    public void muteNotificationWhenPcOnline(boolean isMute, GeneralCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (!defaultSilentWhenPCOnline) {
            isMute = !isMute;
        }
        setUserSetting(UserSettingScope.MuteWhenPcOnline, "", isMute ? "0" : "1", callback);
    }

    /**
     * 创建密聊
     *
     * @param userId
     * @param callback
     */
    public void createSecretChat(String userId, final CreateSecretChatCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.createSecretChat(userId, new ICreateSecretChatCallback.Stub() {
                @Override
                public void onSuccess(String s, int i) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(s, i));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 销毁密聊
     *
     * @param targetId
     * @param callback
     */
    public void destroySecretChat(String targetId, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.destroySecretChat(targetId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess());
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取密聊信息
     *
     * @param targetId
     * @return
     */
    public SecretChatInfo getSecretChatInfo(String targetId) {
        if (!checkRemoteService()) {
            return null;
        }
        try {
            SecretChatInfo secretChatInfo = mClient.getSecretChatInfo(targetId);
            if (secretChatInfo == null) {
                removeConversation(new Conversation(Conversation.ConversationType.SecretChat, targetId, 0), true);
            }
            return secretChatInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密密聊媒体数据
     *
     * @param targetid
     * @param mediaData
     * @return
     */
    public byte[] decodeSecretChatData(String targetid, byte[] mediaData) {
        if (!checkRemoteService()) {
            return new byte[0];
        }
        try {
            return mClient.decodeSecretChatData(targetid, mediaData);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * 解密密聊媒体数据，回调是在工作线程
     *
     * @param targetId
     * @param mediaData
     * @param callback
     */
    public void decodeSecretDataAsync(String targetId, byte[] mediaData, GeneralCallbackBytes callback) {
        if (!checkRemoteService()) {
            return;
        }
        internalWorkHandler.post(() -> {
            try {
                AshmenWrapper ashmenWrapper = AshmenWrapper.create(targetId, mediaData.length);
                ashmenWrapper.writeBytes(mediaData, 0, mediaData.length);
                mClient.decodeSecretChatDataAsync(targetId, ashmenWrapper, mediaData.length, new IGeneralCallbackInt.Stub() {
                    @Override
                    public void onSuccess(int length) throws RemoteException {
                        if (callback != null) {
                            // TODO ByteArrayOutputStream
                            byte[] data = new byte[length];
                            try {
                                ashmenWrapper.readBytes(data, 0, length);
                                callback.onSuccess(data);
                            } finally {
                                ashmenWrapper.close();
                            }
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) throws RemoteException {
                        if (callback != null) {
                            callback.onFail(errorCode);
                        }
                        ashmenWrapper.close();
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 设置密聊消息焚毁时间，单位秒
     *
     * @param targetId
     * @param burnTime
     */
    public void setSecretChatBurnTime(String targetId, int burnTime) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setSecretChatBurnTime(targetId, burnTime);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取消息焚毁信息
     *
     * @param messageId
     * @return
     */
    public BurnMessageInfo getBurnMessageInfo(long messageId) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getBurnMessageInfo(messageId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendConferenceRequest(long sessionId, String roomId, String request, String data, final GeneralCallback2 callback) {
        sendConferenceRequest(sessionId, roomId, request, false, data, callback);
    }

    public void sendConferenceRequest(long sessionId, String roomId, String request, boolean advanced, String data, final GeneralCallback2 callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            Log.d("PCRTCClient", "send conference data:" + request + ": " + data);
            mClient.sendConferenceRequest(sessionId, roomId, request, advanced, data, new cn.wildfirechat.client.IGeneralCallback2.Stub() {
                @Override
                public void onSuccess(String result) throws RemoteException {
                    Log.d("PCRTCClient", "send conference result:" + result);
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(result);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * 从payload转化为消息内容
     *
     * @param payload
     * @param from
     * @return
     */
    public MessageContent messageContentFromPayload(MessagePayload payload, String from) {
        if (rawMsg) {
            RawMessageContent rawMessageContent = new RawMessageContent();
            rawMessageContent.payload = payload;
            return rawMessageContent;
        }

        MessageContent content = null;
        try {
            Class cls = messageContentMap.get(payload.type);
            if (cls != null) {
                content = (MessageContent) cls.newInstance();
            } else {
                content = new UnknownMessageContent();
            }
            if (content instanceof CompositeMessageContent) {
                ((CompositeMessageContent) content).decode(payload, this);
            } else {
                Log.e(TAG, "decode");
                content.decode(payload);
            }
            if (content instanceof NotificationMessageContent) {
                if (content instanceof RecallMessageContent) {
                    RecallMessageContent recallMessageContent = (RecallMessageContent) content;
                    if (recallMessageContent.getOperatorId().equals(userId)) {
                        ((NotificationMessageContent) content).fromSelf = true;
                    }
                } else if (from.equals(userId)) {
                    ((NotificationMessageContent) content).fromSelf = true;
                }
            }
            content.extra = payload.extra;
        } catch (Exception e) {
            Log.e(TAG, "decode message error, fallback to unknownMessageContent. " + payload.type);
            e.printStackTrace();
            if (content == null) {
                return null;
            }
            if (content.getPersistFlag() == PersistFlag.Persist || content.getPersistFlag() == PersistFlag.Persist_And_Count) {
                content = new UnknownMessageContent();
                ((UnknownMessageContent) content).setOrignalPayload(payload);
            } else {
                return null;
            }
        }
        return content;
    }

    /**
     * 设置默认头像生成器
     *
     * @param clazz
     */
    public void setDefaultPortraitProviderClazz(Class<? extends DefaultPortraitProvider> clazz) {
        this.defaultPortraitProviderClazz = clazz;
        if (mClient != null) {
            try {
                mClient.setDefaultPortraitProviderClass(defaultPortraitProviderClazz.getName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 双网环境时，设置媒体内容 url 转换器
     *
     * @param clazz
     */
    public void setUrlRedirectorClazz(Class<? extends UrlRedirector> clazz) {
        this.urlRedirectorClazz = clazz;
        try {
            this.urlRedirector = clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mClient != null) {
            try {
                mClient.setUrlRedirectorClass(clazz.getName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取长连接端口，长连接连接失败时，返回的是 route 返回的 im-server 配置的长连接端口
     *
     * @return 长连接端口
     */
    public int getLongLinkPort() {
        if (mClient != null) {
            try {
                return mClient.getLongLinkPort();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;
    }

    /**
     * 获取 route 请求的错误码
     *
     * @return route 请求错误码
     */
    public int getRouteErrorCode() {
        if (mClient != null) {
            try {
                return mClient.getRouteErrorCode();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;
    }

    /**
     * 上传用户本地角标数字到IM服务，当IM服务推送时，会把此数字发送到推送服务，从而让推送角标显示准确
     *
     * @param number 角标数
     */

    public void uploadBadgeNumber(int number) {
        this.setUserSetting(UserSettingScope.Sync_Badge, "", String.valueOf(number), null);
    }

    private void reportBadgeNumber() {
        UnreadCount totalUnreadCount = ChatManager.Instance().getUnreadCountEx(List.of(Single, Group, Channel, SecretChat), List.of(0));
        int unreadFriendRequest = ChatManager.Instance().getUnreadFriendRequestStatus();
        int count = totalUnreadCount.unread + unreadFriendRequest;
        uploadBadgeNumber(count);
    }

    private boolean checkRemoteService() {
        if (INST != null) {
            if (mClient != null) {
                return true;
            }

            Intent intent = new Intent(gContext, ClientService.class);
            intent.putExtra("clientId", getClientId());
            boolean result = gContext.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
            if (!result) {
                Log.e(TAG, "Bind service failure");
            }
        } else {
            Log.e(TAG, "Chat manager not initialized");
        }

        return false;
    }

    private void cleanLogFiles() {
        List<String> filePaths = ChatManager.Instance().getLogFilesPath();
        if (filePaths == null || filePaths.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long LOG_KEEP_DURATION = 7 * 24 * 60 * 60 * 1000;
        for (String path : filePaths) {
            File file = new File(path);
            if (file.exists() && file.lastModified() > 0 && now - file.lastModified() > LOG_KEEP_DURATION) {
                file.deleteOnExit();
            }
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "marsClientService connected");
            if (iBinder instanceof android.os.Binder) {
                isClientServiceInMainProcess = true;
            } else {
                // BinderProxy
                isClientServiceInMainProcess = false;
            }
            mClient = IRemoteClient.Stub.asInterface(iBinder);
            internalWorkHandler.post(() -> {
                try {
                    if(isPad) {
                        mClient.setPlatform(isPad);
                    }
                    if (useSM4) {
                        mClient.useSM4();
                    }
                    if (useAES256) {
                        mClient.useAES256();
                    }
                    if (tcpShortLink) {
                        mClient.useTcpShortLink();
                    }
                    if (rawMsg) {
                        mClient.useRawMsg();
                    }
                    if (noUseFts) {
                        mClient.noUseFts();
                    }
                    if (checkSignature) {
                        mClient.checkSignature();
                    }
                    if (heartBeatInterval > 0) {
                        mClient.setHeartBeatInterval(heartBeatInterval);
                    }
                    if (timeOffset > 0) {
                        mClient.setTimeOffset(timeOffset);
                    }

                    mClient.setBackupAddressStrategy(backupAddressStrategy);
                    if (!TextUtils.isEmpty(backupAddressHost))
                        mClient.setBackupAddress(backupAddressHost, backupAddressPort);

                    if (proxyInfo != null) {
                        mClient.setProxyInfo(proxyInfo);
                    }

                    mClient.setServerAddress(SERVER_HOST);
                    for (Class clazz : messageContentMap.values()) {
                        mClient.registerMessageContent(clazz.getName());
                    }

                    for (Map.Entry<Integer, PersistFlag> entry : messagePersistFlagMap.entrySet()) {
                        mClient.registerMessageFlag(entry.getKey(), entry.getValue().getValue());
                    }

                    if (startLog) {
                        startLog();
                    } else {
                        stopLog();
                    }

                    if (!TextUtils.isEmpty(deviceToken)) {
                        mClient.setDeviceToken(deviceToken, pushType);
                    }

                    mClient.setForeground(1);
                    mClient.setOnReceiveMessageListener(new IOnReceiveMessageListener.Stub() {
                        @Override
                        public void onReceive(List<Message> messages, boolean hasMore) throws RemoteException {
                            onReceiveMessage(messages, hasMore);
                        }

                        @Override
                        public void onRecall(long messageUid) throws RemoteException {
                            onRecallMessage(messageUid);
                        }

                        @Override
                        public void onDelete(long messageUid) throws RemoteException {
                            onDeleteMessage(messageUid);
                        }

                        @Override
                        public void onDelivered(Map deliveryMap) throws RemoteException {
                            onMsgDelivered(deliveryMap);
                        }

                        @Override
                        public void onReaded(List<ReadEntry> readEntrys) throws RemoteException {
                            onMsgReaded(readEntrys);
                        }
                    });
                    mClient.setOnConnectionStatusChangeListener(new IOnConnectionStatusChangeListener.Stub() {
                        @Override
                        public void onConnectionStatusChange(int connectionStatus) throws RemoteException {
                            ChatManager.this.onConnectionStatusChange(connectionStatus);
                        }
                    });
                    mClient.setOnConnectToServerListener(new IOnConnectToServerListener.Stub() {
                        @Override
                        public void onConnectToServer(String host, String ip, int port) throws RemoteException {
                            ChatManager.this.onConnectToServer(host, ip, port);
                        }
                    });
                    mClient.setOnUserInfoUpdateListener(new IOnUserInfoUpdateListener.Stub() {
                        @Override
                        public void onUserInfoUpdated(List<UserInfo> userInfos) throws RemoteException {
                            ChatManager.this.onUserInfoUpdate(userInfos);
                        }
                    });
                    mClient.setOnGroupInfoUpdateListener(new IOnGroupInfoUpdateListener.Stub() {
                        @Override
                        public void onGroupInfoUpdated(List<GroupInfo> groupInfos) throws RemoteException {
                            ChatManager.this.onGroupInfoUpdated(groupInfos);
                        }
                    });
                    mClient.setOnGroupMembersUpdateListener(new IOnGroupMembersUpdateListener.Stub() {
                        @Override
                        public void onGroupMembersUpdated(String groupId, List<GroupMember> members) throws RemoteException {
                            ChatManager.this.onGroupMembersUpdate(groupId, members);
                        }
                    });
                    mClient.setOnFriendUpdateListener(new IOnFriendUpdateListener.Stub() {
                        @Override
                        public void onFriendListUpdated(List<String> friendList) throws RemoteException {
                            ChatManager.this.onFriendListUpdated(friendList);
                        }

                        @Override
                        public void onFriendRequestUpdated(List<String> newRequests) throws RemoteException {
                            ChatManager.this.onFriendReqeustUpdated(newRequests);
                        }
                    });
                    mClient.setOnSettingUpdateListener(new IOnSettingUpdateListener.Stub() {
                        @Override
                        public void onSettingUpdated() throws RemoteException {
                            ChatManager.this.onSettingUpdated();
                        }
                    });
                    mClient.setOnChannelInfoUpdateListener(new IOnChannelInfoUpdateListener.Stub() {
                        @Override
                        public void onChannelInfoUpdated(List<ChannelInfo> channelInfos) throws RemoteException {
                            ChatManager.this.onChannelInfoUpdate(channelInfos);
                        }
                    });
                    mClient.setOnConferenceEventListener(new IOnConferenceEventListener.Stub() {
                        @Override
                        public void onConferenceEvent(String event) throws RemoteException {
                            ChatManager.this.onConferenceEvent(event);
                        }
                    });
                    mClient.setOnTrafficDataListener(new IOnTrafficDataListener.Stub() {
                        @Override
                        public void onTrafficData(long send, long recv) throws RemoteException {
                            ChatManager.this.onTrafficData(send, recv);
                        }
                    });

                    mClient.setUserOnlineEventListener(new IOnUserOnlineEventListener.Stub() {

                        @Override
                        public void onUserOnlineEvent(UserOnlineState[] states) throws RemoteException {
                            ChatManager.this.onUserOnlineEvent(states);
                        }
                    });

                    mClient.setSecretChatStateChangedListener(new IOnSecretChatStateListener.Stub() {
                        @Override
                        public void onSecretChatStateChanged(String targetid, int state) throws RemoteException {
                            ChatManager.this.onSecretChatStateChanged(targetid, state);
                        }
                    });
                    mClient.setDomainInfoUpdateListener(new IOnDomainInfoUpdateListener.Stub() {
                        @Override
                        public void onDomainInfoUpdated(DomainInfo domainInfo) throws RemoteException {
                            ChatManager.this.onDomainInfoUpdate(domainInfo);
                        }
                    });

                    if (defaultPortraitProviderClazz != null) {
                        mClient.setDefaultPortraitProviderClass(defaultPortraitProviderClazz.getName());
                    }

                    if (urlRedirectorClazz != null) {
                        mClient.setUrlRedirectorClass(urlRedirectorClazz.getName());
                    }

                    mClient.setSecretMessageBurnStateListener(new IOnSecretMessageBurnStateListener.Stub() {
                        @Override
                        public void onSecretMessageStartBurning(String targetId, long playedMsgId) throws RemoteException {
                            ChatManager.this.onSecretMessageStartBurning(targetId, playedMsgId);
                        }

                        @Override
                        public void onSecretMessageBurned(int[] messageIds) throws RemoteException {
                            if (messageIds != null && messageIds.length > 0) {
                                List<Long> arr = new ArrayList<>();
                                for (int i = 0; i < messageIds.length; i++) {
                                    arr.add((long) messageIds[i]);
                                }
                                ChatManager.this.onSecretMessageBurned(arr);
                            }
                        }
                    });

                    mClient.setLiteMode(isLiteMode);
                    mClient.setLowBPSMode(isLowBPSMode);

                    if (!TextUtils.isEmpty(protoUserAgent)) {
                        mClient.setProtoUserAgent(protoUserAgent);
                    }
                    if (!protoHttpHeaderMap.isEmpty()) {
                        for (Map.Entry<String, String> entry : protoHttpHeaderMap.entrySet()) {
                            try {
                                mClient.addHttpHeader(entry.getKey(), entry.getValue());
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(token)) {
                        mClient.connect(userId, token);
                    }

                    int clientConnectionStatus = mClient.getConnectionStatus();
                    if (clientConnectionStatus == ConnectionStatus.ConnectionStatusConnected) {
                        //service刚绑定成功时连接状态就已经时连接成功了，可能是应用出现异常重启了。
                        onConnectionStatusChange(clientConnectionStatus);
                        onSettingUpdated();
                    }

                    mainHandler.post(() -> {
                        for (IMServiceStatusListener listener : imServiceStatusListeners) {
                            listener.onServiceConnected();
                        }
                    });
                } catch (Throwable e) {
                    // 抓住所有异常，发生异常之后，im将不能正常工作，需要重新启动ClientService服务，故这儿抓住所以异常，防止应用crash并没有什么问题
                    Log.e(TAG, "onServiceConnected worker exception" + e.toString());
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "onServiceDisconnected");
            mClient = null;
            checkRemoteService();
            mainHandler.post(() -> {
                for (IMServiceStatusListener listener : imServiceStatusListeners) {
                    listener.onServiceDisconnected();
                }
            });
        }
    };

    private static interface MediaMessageUploadCallback {
        void onMediaMessageUploaded(String remoteUrl);
    }

    private void registerCoreMessageContents() {
        registerMessageContent(AddGroupMemberNotificationContent.class);
        registerMessageContent(CallStartMessageContent.class);
        registerMessageContent(ConferenceInviteMessageContent.class);
        registerMessageContent(ChangeGroupNameNotificationContent.class);
        registerMessageContent(ChangeGroupPortraitNotificationContent.class);
        registerMessageContent(CreateGroupNotificationContent.class);
        registerMessageContent(DismissGroupNotificationContent.class);
        registerMessageContent(FileMessageContent.class);
        registerMessageContent(ImageMessageContent.class);
        registerMessageContent(LinkMessageContent.class);
        registerMessageContent(KickoffGroupMemberNotificationContent.class);
        registerMessageContent(LocationMessageContent.class);
        registerMessageContent(ModifyGroupAliasNotificationContent.class);
        registerMessageContent(ModifyGroupExtraNotificationContent.class);
        registerMessageContent(ModifyGroupSettingsNotificationContent.class);
        registerMessageContent(ModifyGroupMemberExtraNotificationContent.class);
        registerMessageContent(QuitGroupNotificationContent.class);
        registerMessageContent(RecallMessageContent.class);
        registerMessageContent(DeleteMessageContent.class);
        registerMessageContent(SoundMessageContent.class);
        registerMessageContent(StickerMessageContent.class);
        registerMessageContent(TextMessageContent.class);
        registerMessageContent(PCLoginRequestMessageContent.class);
        registerMessageContent(PTextMessageContent.class);
        registerMessageContent(TipNotificationContent.class);
        registerMessageContent(FriendAddedMessageContent.class);
        registerMessageContent(FriendGreetingMessageContent.class);
        registerMessageContent(TransferGroupOwnerNotificationContent.class);
        registerMessageContent(VideoMessageContent.class);
        registerMessageContent(TypingMessageContent.class);
        registerMessageContent(GroupMuteNotificationContent.class);
        registerMessageContent(GroupJoinTypeNotificationContent.class);
        registerMessageContent(GroupPrivateChatNotificationContent.class);
        registerMessageContent(GroupRejectJoinNotificationContent.class);
        registerMessageContent(GroupSetManagerNotificationContent.class);
        registerMessageContent(GroupMuteMemberNotificationContent.class);
        registerMessageContent(GroupAllowMemberNotificationContent.class);
        registerMessageContent(KickoffGroupMemberVisibleNotificationContent.class);
        registerMessageContent(QuitGroupVisibleNotificationContent.class);
        registerMessageContent(CardMessageContent.class);
        registerMessageContent(CompositeMessageContent.class);
        registerMessageContent(MarkUnreadMessageContent.class);
        registerMessageContent(PTTSoundMessageContent.class);
        registerMessageContent(StartSecretChatMessageContent.class);
        registerMessageContent(EnterChannelChatMessageContent.class);
        registerMessageContent(LeaveChannelChatMessageContent.class);
        registerMessageContent(MultiCallOngoingMessageContent.class);
        registerMessageContent(JoinCallRequestMessageContent.class);
        registerMessageContent(RichNotificationMessageContent.class);
        registerMessageContent(ArticlesMessageContent.class);
        registerMessageContent(ChannelMenuEventMessageContent.class);
        registerMessageContent(StreamingTextGeneratingMessageContent.class);
        registerMessageContent(StreamingTextGeneratedMessageContent.class);
        registerMessageContent(NotDeliveredMessageContent.class);
    }

    private MessageContent contentOfType(int type) {
        Class<? extends MessageContent> cls = messageContentMap.get(type);
        if (cls != null) {
            try {
                return cls.newInstance();
            } catch (Exception e) {
                Log.e(TAG, "create message content instance failed, fall back to UnknownMessageContent, the message content class must have a default constructor. " + type);
                e.printStackTrace();
            }
        }
        return new UnknownMessageContent();
    }

    private static int[] convertIntegers(List<Integer> integers) {
        if (integers == null) {
            return new int[0];
        }

        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }

    private static boolean checkSDKHost(String host) {
        Class clazz;
        Method method;
        boolean result;
        Log.d(TAG, "*************** SDK检查 *****************");
        try {
            clazz = Class.forName("cn.wildfirechat.avenginekit.AVEngineKit");
            method = clazz.getMethod("isSupportMultiCall");
            boolean multiCall = (boolean) method.invoke(null);
            method = clazz.getMethod("isSupportConference");
            boolean conference = (boolean) method.invoke(null);
            if (conference) {
                Log.e(TAG, "音视频SDK是高级版");
            } else if (multiCall) {
                Log.e(TAG, "音视频SDK是多人版");
            } else {
                Log.e(TAG, "音视频SDK是单人版");
            }
        } catch (Exception e) {
            // do nothing
            Log.e(TAG, "检查音视频 SDK 失败: " + e.getMessage());
        }

        try {
            clazz = Class.forName("cn.wildfirechat.moment.MomentClient");
            method = clazz.getMethod("checkAddress", String.class);
            result = (boolean) method.invoke(null, host);
            if (!result) {
                Log.e(TAG, "错误，朋友圈SDK跟域名不匹配。请检查SDK的授权域名是否与当前使用的域名一致。");
            }
        } catch (Exception e) {
            // do nothing
            Log.e(TAG, "检查朋友圈 SDK 失败。不支持朋友圈功能，但不会影响其他功能! " + e.getMessage());
        }

        try {

            clazz = Class.forName("cn.wildfirechat.ptt.PTTClient");
            method = clazz.getMethod("checkAddress", String.class);
            result = (boolean) method.invoke(null, host);
            if (!result) {
                Log.e(TAG, "错误，对讲SDK跟域名不匹配。请检查SDK的授权域名是否与当前使用的域名一致。");
            }
        } catch (Exception e) {
            Log.e(TAG, "检查对讲 SDK 失败。不支持对讲功能（该功能不是发送语音消息），但不会影响其他功能！ " + e.getMessage());
        }
        Log.d(TAG, "*************** SDK检查 *****************");
        return true;
    }

    private String getExternalUserDisplayName(String userId) {
        String domainId = WfcUtils.getExternalDomainId(userId);
        DomainInfo domainInfo = ChatManager.Instance().getDomainInfo(domainId, false);
        String domainName = domainInfo != null ? domainInfo.name : "<" + domainId + ">";
        UserInfo userInfo = getUserInfo(userId, false);
        String userDisplayName = TextUtils.isEmpty(userInfo.displayName) ? "<" + userId + ">" : userInfo.displayName;
        return userDisplayName + " @" + domainName;
    }
}
