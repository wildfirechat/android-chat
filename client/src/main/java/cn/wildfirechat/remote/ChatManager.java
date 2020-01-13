package cn.wildfirechat.remote;


import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.wildfirechat.ErrorCode;
import cn.wildfirechat.UserSource;
import cn.wildfirechat.client.ClientService;
import cn.wildfirechat.client.ICreateChannelCallback;
import cn.wildfirechat.client.IGeneralCallback;
import cn.wildfirechat.client.IGetRemoteMessageCallback;
import cn.wildfirechat.client.IOnChannelInfoUpdateListener;
import cn.wildfirechat.client.IOnConnectionStatusChangeListener;
import cn.wildfirechat.client.IOnFriendUpdateListener;
import cn.wildfirechat.client.IOnGroupInfoUpdateListener;
import cn.wildfirechat.client.IOnGroupMembersUpdateListener;
import cn.wildfirechat.client.IOnReceiveMessageListener;
import cn.wildfirechat.client.IOnSettingUpdateListener;
import cn.wildfirechat.client.IOnUserInfoUpdateListener;
import cn.wildfirechat.client.IRemoteClient;
import cn.wildfirechat.client.IUploadMediaCallback;
import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.notification.DismissGroupNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.QuitGroupNotificationContent;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.ChatRoomInfo;
import cn.wildfirechat.model.ChatRoomMembersInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.ConversationSearchResult;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.model.ModifyChannelInfoType;
import cn.wildfirechat.model.ModifyGroupInfoType;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.NullChannelInfo;
import cn.wildfirechat.model.NullGroupInfo;
import cn.wildfirechat.model.NullUserInfo;
import cn.wildfirechat.model.UnreadCount;
import cn.wildfirechat.model.UserInfo;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by WF Chat on 2017/12/11.
 */

public class ChatManager {
    private static final String TAG = ChatManager.class.getName();

    private String SERVER_HOST;

    private static IRemoteClient mClient;

    private static ChatManager INST;
    private static Context gContext;

    private String userId;
    private String token;
    private Handler mainHandler;
    private Handler workHandler;
    private String deviceToken;
    private String clientId;
    private int pushType;
    private List<String> msgList = new ArrayList<>();

    private UserSource userSource;

    private boolean startLog;
    private int connectionStatus;

    private boolean isBackground = true;
    private List<OnReceiveMessageListener> onReceiveMessageListeners = new ArrayList<>();
    private List<OnConnectionStatusChangeListener> onConnectionStatusChangeListeners = new ArrayList<>();
    private List<OnSendMessageListener> sendMessageListeners = new ArrayList<>();
    private List<OnGroupInfoUpdateListener> groupInfoUpdateListeners = new ArrayList<>();
    private List<OnGroupMembersUpdateListener> groupMembersUpdateListeners = new ArrayList<>();
    private List<OnUserInfoUpdateListener> userInfoUpdateListeners = new ArrayList<>();
    private List<OnSettingUpdateListener> settingUpdateListeners = new ArrayList<>();
    private List<OnFriendUpdateListener> friendUpdateListeners = new ArrayList<>();
    private List<OnConversationInfoUpdateListener> conversationInfoUpdateListeners = new ArrayList<>();
    private List<OnRecallMessageListener> recallMessageListeners = new ArrayList<>();
    private List<RemoveMessageListener> removeMessageListeners = new ArrayList<>();
    private List<OnChannelInfoUpdateListener> channelInfoUpdateListeners = new ArrayList<>();
    private List<OnMessageUpdateListener> messageUpdateListeners = new ArrayList<>();
    private List<OnClearMessageListener> clearMessageListeners = new ArrayList<>();
    private List<OnRemoveConversationListener> removeConversationListeners = new ArrayList<>();

    private List<IMServiceStatusListener> imServiceStatusListeners = new ArrayList<>();

    // key = userId
    private LruCache<String, UserInfo> userInfoCache;
    // key = memberId@groupId
    private LruCache<String, GroupMember> groupMemberCache;

    public enum SearchUserType {
        //模糊搜索displayName，精确搜索name或电话号码
        General(0),

        //精确搜索name或电话号码
        NameOrMobile(1),

        //精确搜索name
        Name(2),

        //精确搜索电话号码
        Mobile(3);

        private int value;

        SearchUserType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
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
     * 初始化，must be called from the main thread
     * serverHost可以是IP，可以是域名，如果是域名的话只支持主域名或www域名，二级域名不支持！
     * 例如：example.com或www.example.com是支持的；xx.example.com或xx.yy.example.com是不支持的。
     *
     * @param context
     * @param serverHost
     * @return
     */

    public static void init(Application context, String serverHost) {
        if (INST != null) {
            // TODO: Already initialized
            return;
        }
        gContext = context.getApplicationContext();
        INST = new ChatManager(serverHost);
        INST.mainHandler = new Handler();
        HandlerThread thread = new HandlerThread("workHandler");
        thread.start();
        INST.workHandler = new Handler(thread.getLooper());
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
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        INST.checkRemoteService();

        SharedPreferences sp = gContext.getSharedPreferences("wildfirechat.config", Context.MODE_PRIVATE);
        INST.userId = sp.getString("userId", null);
        INST.token = sp.getString("token", null);
    }

    public Context getApplicationContext() {
        return gContext;
    }

    /**
     * 当有自己的用户账号体系，不想使用火信提供的用户信息托管服务时，调用此方法设置用户信息源
     *
     * @param userSource 用户信息源
     */
    public void setUserSource(UserSource userSource) {
        this.userSource = userSource;
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    //App在后台时，如果需要强制连上服务器并收消息，调用此方法。后台时无法保证长时间连接中。
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
     * 消息被撤回
     *
     * @param messageUid
     */
    private void onRecallMessage(final long messageUid) {
        Message message = getMessageByUid(messageUid);
        // 想撤回的消息已经被删除
        if (message == null) {
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (OnRecallMessageListener listener : recallMessageListeners) {
                    listener.onRecallMessage(message);
                }
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

    private void onFriendReqeustUpdated() {
        mainHandler.post(() -> {
            for (OnFriendUpdateListener listener : friendUpdateListeners) {
                listener.onFriendRequestUpdate();
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

    public void addGroupMembersUpdateListener(OnGroupMembersUpdateListener listener) {
        if (listener != null) {
            groupMembersUpdateListeners.add(listener);
        }
    }

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
     * 获取clientId, 火信用clientId唯一表示用户设备
     */
    public String getClientId() {
        if (clientId != null) {
            return clientId;
        }
        String imei = PreferenceManager.getDefaultSharedPreferences(gContext).getString("mars_core_uid", "");
        if (TextUtils.isEmpty(imei)) {
            imei = Settings.Secure.getString(gContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (TextUtils.isEmpty(imei)) {
                imei = UUID.randomUUID().toString();
            }
            imei += System.currentTimeMillis();
            PreferenceManager.getDefaultSharedPreferences(gContext).edit().putString("mars_core_uid", imei).commit();
        }
        clientId = imei;
        return clientId;
    }

    /**
     * 创建频道
     *
     * @param channelId       频道id，如果传null，火信会自动生成id；否则，使用用户提供的id，需要保证此id的唯一性
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
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
     * 添加主动删除消息监听
     *
     * @param listener
     */
    public void addRemoveMessageListener(RemoveMessageListener listener) {
        if (listener == null) {
            return;
        }
        removeMessageListeners.add(listener);
    }

    /**
     * 删除删除消息监听
     *
     * @param listener
     */
    public void removeRemoveMessageListener(RemoveMessageListener listener) {
        removeMessageListeners.remove(listener);
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

    public void addClearMessageListener(OnClearMessageListener listener) {
        if (listener == null) {
            return;
        }

        clearMessageListeners.add(listener);
    }

    public void removeClearMessageListener(OnClearMessageListener listener) {
        clearMessageListeners.remove(listener);
    }

    public void addRemoveConversationListener(OnRemoveConversationListener listener) {
        if (listener == null) {
            return;
        }
        removeConversationListeners.add(listener);
    }

    public void removeRemoveConversationListener(OnRemoveConversationListener listener) {
        removeConversationListeners.remove(listener);
    }


    public void addIMServiceStatusListener(IMServiceStatusListener listener) {
        if (listener == null) {
            return;
        }
        imServiceStatusListeners.add(listener);
    }

    public void removeIMServiceStatusListener(IMServiceStatusListener listener) {
        imServiceStatusListeners.remove(listener);
    }

    private void validateMessageContent(Class<? extends MessageContent> msgContentClazz) {
        try {
            Constructor c = msgContentClazz.getConstructor();
            if (c.getModifiers() != Modifier.PUBLIC) {
                throw new IllegalArgumentException("the default constructor of your custom messageContent class should be public");
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("custom messageContent class must have a default constructor");
        }
        ContentTag tag = msgContentClazz.getAnnotation(ContentTag.class);
        if (tag == null) {
            throw new IllegalArgumentException("custom messageContent class must have a ContentTag annotation");
        }
    }

    /**
     * 注册自自定义消息
     *
     * @param msgContentCls 自定义消息实现类，可参考自定义消息文档
     */
    public void registerMessageContent(Class<? extends MessageContent> msgContentCls) {

        validateMessageContent(msgContentCls);
        msgList.add(msgContentCls.getName());
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
     * 插入消息
     *
     * @param conversation 目标会话
     * @param sender       消息发送者id
     * @param content      消息体
     * @param status       消息状态
     * @param notify       是否通知界面，通知时，会通过{@link #onReceiveMessage(List, boolean)}通知界面
     * @param serverTime   服务器时间
     * @return
     */
    public Message insertMessage(Conversation conversation, String sender, MessageContent content, MessageStatus status, boolean notify, long serverTime) {
        if (!checkRemoteService()) {
            return null;
        }

        Message message = new Message();
        message.conversation = conversation;
        message.content = content;
        message.sender = sender;
        message.status = status;
        message.serverTime = serverTime;
        if (this.userId.equals(sender)) {
            message.direction = MessageDirection.Send;
        } else {
            message.direction = MessageDirection.Receive;
        }

        try {
            message = mClient.insertMessage(message, notify);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }

        return message;
    }

    /**
     * 更新消息
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
            message.content = newMsgContent;
            boolean result = mClient.updateMessage(message);
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
     * 连接服务器
     * userId和token都不允许为空
     * 需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
     *
     * @param userId
     * @param token
     * @return 是否是新用户。新用户需要同步信息，耗时较长，可以增加等待提示。
     */
    public boolean connect(String userId, String token) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(token)) {
            throw new IllegalArgumentException("userId and token must be empty!");
        }
        this.userId = userId;
        this.token = token;
        this.userInfoCache = new LruCache<>(1024);
        this.groupMemberCache = new LruCache<>(1024);

        if (mClient != null) {
            try {
                SharedPreferences sp = gContext.getSharedPreferences("wildfirechat.config", Context.MODE_PRIVATE);
                sp.edit()
                        .putString("userId", userId)
                        .putString("token", token)
                        .commit();
                return mClient.connect(this.userId, this.token);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 主动断开连接
     *
     * @param cleanSession 是否清除会话session，清除之后，所有之前的会话信息会被删除
     */
    public void disconnect(boolean cleanSession) {
        if (mClient != null) {
            try {
                mClient.disconnect(cleanSession);
                SharedPreferences sp = gContext.getSharedPreferences("wildfirechat.config", Context.MODE_PRIVATE);
                sp.edit().clear().commit();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            this.userId = null;
            this.token = null;
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

        if (msg.content instanceof MediaMessageContent) {
            String localPath = ((MediaMessageContent) msg.content).localPath;
            if (!TextUtils.isEmpty(localPath)) {
                File file = new File(localPath);
                if (!file.exists()) {
                    if (callback != null) {
                        callback.onFail(ErrorCode.FILE_NOT_EXIST);
                    }
                    return;
                }

                if (file.length() > 100 * 1024 * 1024) {
                    if (callback != null) {
                        callback.onFail(ErrorCode.FILE_TOO_LARGE);
                    }
                    return;
                }
            }
        }

        try {
            mClient.send(msg, new cn.wildfirechat.client.ISendMessageCallback.Stub() {
                @Override
                public void onSuccess(final long messageUid, final long timestamp) throws RemoteException {
                    msg.messageUid = messageUid;
                    msg.serverTime = timestamp;
                    msg.status = MessageStatus.Sent;
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
                    MediaMessageContent mediaMessageContent = (MediaMessageContent) msg.content;
                    mediaMessageContent.remoteUrl = remoteUrl;
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
            }, expireDuration);
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                msg.status = MessageStatus.Send_Failure;
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
            }
            for (OnSendMessageListener listener : sendMessageListeners) {
                listener.onSendFail(msg, ErrorCode.SERVICE_EXCEPTION);
            }
        }
    }

    /**
     * 消息撤回
     *
     * @param msg      想撤回的消息
     * @param callback 撤回回调
     */
    public void recallMessage(final Message msg, final GeneralCallback callback) {
        try {
            mClient.recall(msg.messageUid, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    msg.content = new RecallMessageContent(userId, msg.messageUid);
                    ((RecallMessageContent) msg.content).fromSelf = true;
                    callback.onSuccess();
                    for (OnRecallMessageListener listener : recallMessageListeners) {
                        listener.onRecallMessage(msg);
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    callback.onFail(errorCode);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
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
     *
     * @param conversationTypes 获取哪些类型的会话
     * @param lines             获取哪些会话线路
     * @return
     */
    @NonNull
    public List<ConversationInfo> getConversationList(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
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
            return mClient.getConversationList(intypes, inlines);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * 获取会话信息
     *
     * @param conversation
     * @return
     */
    public @Nullable
    ConversationInfo getConversation(Conversation conversation) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return null;
        }


        try {
            return mClient.getConversation(conversation.type.getValue(), conversation.target, conversation.line);
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
     * @param before       true, 获取fromInde之前的消息，即更旧的消息；false，获取fromIndex之后的消息，即更新的消息。都不包含fromIndex对应的消息
     * @param count        获取消息条数
     * @param withUser     只有会话类型为{@link cn.wildfirechat.model.Conversation.ConversationType#Channel}时生效, channel主用来查询和某个用户的所有消息
     * @return
     */
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
        int[] inlines = new int[lines.size()];
        int[] inCntTypes = new int[contentTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        for (int k = 0; k < contentTypes.size(); k++) {
            inCntTypes[k] = contentTypes.get(k);
        }

        try {
            return mClient.getMessagesEx(intypes, inlines, inCntTypes, fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Message> getMessagesEx2(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, MessageStatus messageStatus, long fromIndex, boolean before, int count, String withUser) {
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
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            return mClient.getMessagesEx2(intypes, inlines, messageStatus == null ? -1 : messageStatus.value(), fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void getRemoteMessages(Conversation conversation, long beforeMessageId, int count, GetRemoteMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.getRemoteMessages(conversation, beforeMessageId, count, new IGetRemoteMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages) throws RemoteException {
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
     *                  切和消息的存储类型{@link cn.wildfirechat.message.core.PersistFlag}相关，只有存储类型为
     *                  {@link cn.wildfirechat.message.core.PersistFlag#Persist_And_Count}
     *                  和{@link cn.wildfirechat.message.core.PersistFlag#Persist}的消息，有消息id
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
     *                   切和消息的存储类型{@link cn.wildfirechat.message.core.PersistFlag}相关，只有存储类型为
     *                   {@link cn.wildfirechat.message.core.PersistFlag#Persist_And_Count}
     *                   和{@link cn.wildfirechat.message.core.PersistFlag#Persist}的消息，有消息id
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
    public void clearUnreadStatus(Conversation conversation) {
        if (!checkRemoteService()) {
            return;
        }

        try {

            if (mClient.clearUnreadStatus(conversation.type.getValue(), conversation.target, conversation.line)) {
                ConversationInfo conversationInfo = getConversation(conversation);
                conversationInfo.unreadCount = new UnreadCount();
                for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                    listener.onConversationUnreadStatusClear(conversationInfo);
                }
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void clearUnreadStatusEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        if (!checkRemoteService()) {
            return;
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
            mClient.clearUnreadStatusEx(inTypes, inLines);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置audio获取media等媒体消息的播放状态
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
     * 清除所有会话的未读状态
     */
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
     * 会话置顶
     *
     * @param conversation
     * @param top          true，置顶；false，取消置顶
     */
    public void setConversationTop(Conversation conversation, boolean top) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setConversationTop(conversation.type.ordinal(), conversation.target, conversation.line, top);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        ConversationInfo conversationInfo = getConversation(conversation);
        conversationInfo.isTop = top;
        for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
            listener.onConversationTopUpdate(conversationInfo, top);
        }
    }

    /**
     * 设置会话草稿
     *
     * @param conversation
     * @param draft
     */
    public void setConversationDraft(Conversation conversation, @Nullable String draft) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setConversationDraft(conversation.type.ordinal(), conversation.target, conversation.line, draft);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // 由于aidl中将setConversationDraft定义为oneway, 该调用是异步调用, 所以调用setConversationDraft之后，立即调用getConversation,
        // 获取到的信息，可能还是更新之前的。两个解决方案：
        // 1. 将oneway去掉
        // 2. 这儿强制置一个值
        // 这儿采用第二种方案
        ConversationInfo conversationInfo = getConversation(conversation);
        if (conversationInfo == null) {
            return;
        }
        conversationInfo.draft = draft;
        for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
            listener.onConversationDraftUpdate(conversationInfo, draft);
        }
    }

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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

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

    // 优先级如下：
    // 1. 群备注 2. 好友备注 3. 用户displayName 4. <uid>
    public String getGroupMemberDisplayName(String groupId, String memberId) {
        UserInfo userInfo = getUserInfo(memberId, groupId, false);
        if (userInfo == null) {
            return "<" + memberId + ">";
        }
        if (!TextUtils.isEmpty(userInfo.groupAlias)) {
            return userInfo.groupAlias;
        } else if (!TextUtils.isEmpty(userInfo.friendAlias)) {
            return userInfo.friendAlias;
        } else if (!TextUtils.isEmpty(userInfo.displayName)) {
            return userInfo.displayName;
        }
        return "<" + memberId + ">";
    }

    public String getUserDisplayName(UserInfo userInfo) {
        if (userInfo == null) {
            return "";
        }
        if (!TextUtils.isEmpty(userInfo.friendAlias)) {
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

    public void setFriendAlias(String userId, String alias, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
        }

        try {
            mClient.setFriendAlias(userId, alias, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
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

    public List<UserInfo> getMyFriendListInfo(boolean refresh) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMyFriendListInfo(refresh);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

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

    public List<FriendRequest> getFriendRequest(boolean incomming) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getFriendRequest(incomming);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

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

    public void sendFriendRequest(String userId, String reason, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.sendFriendRequest(userId, reason, new cn.wildfirechat.client.IGeneralCallback.Stub() {
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

    public void handleFriendRequest(String userId, boolean accept, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.handleFriendRequest(userId, accept, new cn.wildfirechat.client.IGeneralCallback.Stub() {
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }


    public @Nullable
    GroupInfo getGroupInfo(String groupId, boolean refresh) {
        if (!checkRemoteService()) {
            return new NullGroupInfo(groupId);
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

    public void joinChatRoom(String chatRoomId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.joinChatRoom(chatRoomId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
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

    public void quitChatRoom(String chatRoomId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.quitChatRoom(chatRoomId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
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
            callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    public void getChatRoomInfo(String chatRoomId, long updateDt, GetChatRoomInfoCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getChatRoomInfo(chatRoomId, updateDt, new cn.wildfirechat.client.IGetChatRoomInfoCallback.Stub() {
                @Override
                public void onSuccess(ChatRoomInfo chatRoomInfo) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(chatRoomInfo));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    public void getChatRoomMembersInfo(String chatRoomId, int maxCount, GetChatRoomMembersInfoCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.getChatRoomMembersInfo(chatRoomId, maxCount, new cn.wildfirechat.client.IGetChatRoomMembersInfoCallback.Stub() {
                @Override
                public void onSuccess(ChatRoomMembersInfo chatRoomMembersInfo) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(chatRoomMembersInfo));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }


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
     */
    public UserInfo getUserInfo(String userId, String groupId, boolean refresh) {
        if (TextUtils.isEmpty(userId)) {
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
     * 返回的list里面的元素可能为null
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
            int step = 400;
            int startIndex, endIndex;
            for (int i = 0; i <= userIds.size() / step; i++) {
                startIndex = i * step;
                endIndex = (i + 1) * step;
                endIndex = endIndex > userIds.size() ? userIds.size() : endIndex;
                List<UserInfo> us = mClient.getUserInfos(userIds.subList(startIndex, endIndex), groupId);
                userInfos.addAll(us);
            }
            if (userInfos != null && userInfos.size() > 0) {
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

    public void uploadMediaFile(String mediaPath, int mediaType, final UploadMediaCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        File file = new File(mediaPath);
        if (!file.exists()) {
            callback.onFail(ErrorCode.FILE_NOT_EXIST);
            return;
        }
        if (file.length() > 900 * 1024) {
            callback.onFail(ErrorCode.FILE_TOO_LARGE);
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }


    /**
     * @param data      不能超过1M，为了安全，实际只有900K
     * @param mediaType
     * @param callback
     */
    public void uploadMedia(String fileName, byte[] data, int mediaType, final GeneralCallback2 callback) {
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

    // 修改个人信息，好像不能多端同步
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }

    }

    public boolean deleteMessage(Message message) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            mClient.deleteMessage(message.messageId);
            for (RemoveMessageListener listener : removeMessageListeners) {
                listener.onMessagedRemove(message);
            }
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }

    }

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

    public List<Message> searchMessage(Conversation conversation, String keyword) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchMessage(conversation, keyword);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

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

    public void createGroup(String groupId, String groupName, String groupPortrait, GroupInfo.GroupType groupType, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback2 callback) {
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
            mClient.createGroup(groupId, groupName, groupPortrait, groupType.value(), memberIds, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback2.Stub() {
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    public void addGroupMembers(String groupId, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
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
            mClient.addGroupMembers(groupId, memberIds, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
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

    private MessagePayload content2Payload(MessageContent content) {
        if (content == null) {
            return null;
        }
        MessagePayload payload = content.encode();
        payload.contentType = content.getClass().getAnnotation(ContentTag.class).type();
        return payload;
    }

    public void removeGroupMembers(String groupId, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    // FIXME: 2018/5/19 notifyMsg应当是在这儿组装，上层只用传几个必要参数吧
    public void quitGroup(String groupId, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
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
            mClient.quitGroup(groupId, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
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

    public void dismissGroup(String groupId, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    public void modifyGroupInfo(String groupId, ModifyGroupInfoType modifyType, String newValue, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
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
            mClient.modifyGroupInfo(groupId, modifyType.ordinal(), newValue, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
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

    // Attention
    // 回调成功只表示服务器修改成功了，本地可能还没更新，等本地将服务器端的改动拉下来之后，会有更新通知回调
    public void modifyGroupAlias(String groupId, String alias, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    public List<GroupMember> getGroupMembers(String groupId, boolean forceUpdate) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getGroupMembers(groupId, forceUpdate);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String groupMemberCacheKey(String groupId, String memberId) {
        return memberId + "@" + groupId;
    }

    public GroupMember getGroupMember(String groupId, String memberId) {
        if (TextUtils.isEmpty(groupId) || TextUtils.isEmpty(memberId)) {
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

    public void transferGroup(String groupId, String newOwner, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    public void setGroupManager(String groupId, boolean isSet, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
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
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
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

    public void getMyGroups(final GetGroupsCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        workHandler.post(() -> {
            Map<String, String> groupIdMap = getUserSettings(UserSettingScope.FavoriteGroup);
            String fav = "1";
            new HashMap<String, String>(1);
            List<GroupInfo> groups = new ArrayList<>();
            if (groupIdMap != null && !groupIdMap.isEmpty()) {
                for (Map.Entry<String, String> entry : groupIdMap.entrySet()) {
                    if (entry.getValue().equals(fav)) {
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

    public void setConversationSilent(Conversation conversation, boolean silent) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setConversationSilent(conversation.type.ordinal(), conversation.target, conversation.line, silent);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        ConversationInfo conversationInfo = getConversation(conversation);
        for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
            listener.onConversationSilentUpdate(conversationInfo, silent);
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

    public boolean begainTransaction() {
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

    public void commitTransaction() {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.commitTransaction();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * IM服务进程是否bind成功
     *
     * @return
     */
    public boolean isIMServiceConnected() {
        return mClient != null;
    }

    public void startLog() {
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

    public void stopLog() {
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

    private String getLogPath() {
        return gContext.getCacheDir().getAbsolutePath() + "/log";
    }

    public List<String> getLogFilesPath() {
        List<String> paths = new ArrayList<>();
        String path = getLogPath();

        //遍历path目录下的所有日志文件，以wflog开头的
        File dir = new File(path);
        File[] subFile = dir.listFiles();
        for (File file : subFile) {
            //wflog为ChatService中定义的，如果修改需要对应修改
            if (file.isFile() && file.getName().startsWith("wflog_")) {
                paths.add(file.getAbsolutePath());
            }
        }
        return paths;
    }

    public void setDeviceToken(String token, int pushType) {
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

    public boolean isGlobalSlient() {
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

    public void setGlobalSlient(boolean isSlient, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.GlobalSilent, "", isSlient ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    callback.onFail(errorCode);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

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

    public void setHiddenNotificationDetail(boolean isSlient, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.HiddenNotificationDetail, "", isSlient ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    callback.onFail(errorCode);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mClient = IRemoteClient.Stub.asInterface(iBinder);
            try {
                mClient.setServerAddress(SERVER_HOST);
                for (String msgName : msgList) {
                    mClient.registerMessageContent(msgName);
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
                });
                mClient.setOnConnectionStatusChangeListener(new IOnConnectionStatusChangeListener.Stub() {
                    @Override
                    public void onConnectionStatusChange(int connectionStatus) throws RemoteException {
                        ChatManager.this.onConnectionStatusChange(connectionStatus);
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
                    public void onFriendRequestUpdated() throws RemoteException {
                        ChatManager.this.onFriendReqeustUpdated();
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

                if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(token)) {
                    mClient.connect(userId, token);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mainHandler.post(() -> {
                for (IMServiceStatusListener listener : imServiceStatusListeners) {
                    listener.onServiceConnected();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("chatManager", "onServiceDisconnected");
            mClient = null;
            checkRemoteService();
            mainHandler.post(() -> {
                for (IMServiceStatusListener listener : imServiceStatusListeners) {
                    listener.onServiceConnected();
                }
            });
        }
    };
}
