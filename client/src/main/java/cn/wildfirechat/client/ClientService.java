package cn.wildfirechat.client;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.LocaleList;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tencent.mars.BaseEvent;
import com.tencent.mars.Mars;
import com.tencent.mars.app.AppLogic;
import com.tencent.mars.proto.ProtoLogic;
import com.tencent.mars.sdt.SdtLogic;
import com.tencent.mars.stn.StnLogic;
import com.tencent.mars.xlog.Log;
import com.tencent.mars.xlog.Xlog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import cn.wildfirechat.message.CallStartMessageContent;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.ImageTextMessageContent;
import cn.wildfirechat.message.LocationMessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.StickerMessageContent;
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
import cn.wildfirechat.message.notification.DismissGroupNotificationContent;
import cn.wildfirechat.message.notification.GroupJoinTypeNotificationContent;
import cn.wildfirechat.message.notification.GroupMuteNotificationContent;
import cn.wildfirechat.message.notification.GroupPrivateChatNotificationContent;
import cn.wildfirechat.message.notification.GroupSetManagerChatNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupAliasNotificationContent;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.message.notification.QuitGroupNotificationContent;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.message.notification.TipNotificationContent;
import cn.wildfirechat.message.notification.TransferGroupOwnerNotificationContent;
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
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.NullGroupMember;
import cn.wildfirechat.model.NullUserInfo;
import cn.wildfirechat.model.ProtoChannelInfo;
import cn.wildfirechat.model.ProtoChatRoomInfo;
import cn.wildfirechat.model.ProtoChatRoomMembersInfo;
import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoConversationSearchresult;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.model.ProtoGroupSearchResult;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoMessageContent;
import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.model.UnreadCount;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.RecoverReceiver;

import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnected;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusLogout;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusUnconnected;
import static cn.wildfirechat.remote.UserSettingScope.ConversationSilent;
import static cn.wildfirechat.remote.UserSettingScope.ConversationTop;
import static com.tencent.mars.comm.PlatformComm.context;


/**
 * Created by heavyrain lee on 2017/11/19.
 */

public class ClientService extends Service implements SdtLogic.ICallBack,
        AppLogic.ICallBack,
        ProtoLogic.IConnectionStatusCallback,
        ProtoLogic.IReceiveMessageCallback,
        ProtoLogic.IUserInfoUpdateCallback,
        ProtoLogic.ISettingUpdateCallback,
        ProtoLogic.IFriendRequestListUpdateCallback,
        ProtoLogic.IFriendListUpdateCallback,
        ProtoLogic.IGroupInfoUpdateCallback,
        ProtoLogic.IChannelInfoUpdateCallback, ProtoLogic.IGroupMembersUpdateCallback {
    private Map<Integer, Class<? extends MessageContent>> contentMapper = new HashMap<>();

    private int mConnectionStatus;
    private String mBackupDeviceToken;
    private int mBackupPushType;

    private Handler handler;

    private boolean logined;
    private String userId;
    private RemoteCallbackList<IOnReceiveMessageListener> onReceiveMessageListeners = new WfcRemoteCallbackList<>();
    private RemoteCallbackList<IOnConnectionStatusChangeListener> onConnectionStatusChangeListenes = new WfcRemoteCallbackList<>();
    private RemoteCallbackList<IOnFriendUpdateListener> onFriendUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private RemoteCallbackList<IOnUserInfoUpdateListener> onUserInfoUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private RemoteCallbackList<IOnGroupInfoUpdateListener> onGroupInfoUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private RemoteCallbackList<IOnSettingUpdateListener> onSettingUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private RemoteCallbackList<IOnChannelInfoUpdateListener> onChannelInfoUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private RemoteCallbackList<IOnGroupMembersUpdateListener> onGroupMembersUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();

    private AppLogic.AccountInfo accountInfo = new AppLogic.AccountInfo();
    //        public final String DEVICE_NAME = android.os.Build.MANUFACTURER + "-" + android.os.Build.MODEL;
    public String DEVICE_TYPE = "Android";//"android-" + android.os.Build.VERSION.SDK_INT;
    private AppLogic.DeviceInfo info;

    private int clientVersion = 200;
    private static final String TAG = "ClientService";

    private BaseEvent.ConnectionReceiver mConnectionReceiver;

    private String mHost;
    private int mPort;

    private class ClientServiceStub extends IRemoteClient.Stub {

        @Override
        public String getClientId() throws RemoteException {
            return getDeviceType().clientid;
        }

        @Override
        public boolean connect(String userName, String userPwd) throws RemoteException {
            if (logined) {
                return false;
            }

            logined = true;
            accountInfo.userName = userName;

            mConnectionStatus = ConnectionStatusUnconnected;
            userId = userName;
            return initProto(userName, userPwd);
        }

        @Override
        public void setOnReceiveMessageListener(IOnReceiveMessageListener listener) throws RemoteException {
            onReceiveMessageListeners.register(listener);
        }

        @Override
        public void setOnConnectionStatusChangeListener(IOnConnectionStatusChangeListener listener) throws RemoteException {
            onConnectionStatusChangeListenes.register(listener);
        }


        @Override
        public void setOnUserInfoUpdateListener(IOnUserInfoUpdateListener listener) throws RemoteException {
            onUserInfoUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnGroupInfoUpdateListener(IOnGroupInfoUpdateListener listener) throws RemoteException {
            onGroupInfoUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnFriendUpdateListener(IOnFriendUpdateListener listener) throws RemoteException {
            onFriendUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnSettingUpdateListener(IOnSettingUpdateListener listener) throws RemoteException {
            onSettingUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnChannelInfoUpdateListener(IOnChannelInfoUpdateListener listener) throws RemoteException {
            onChannelInfoUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnGroupMembersUpdateListener(IOnGroupMembersUpdateListener listener) throws RemoteException {
            onGroupMembersUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void disconnect(boolean clearSession) throws RemoteException {
            logined = false;
            userId = null;
            mConnectionStatus = ConnectionStatusLogout;
            onConnectionStatusChanged(ConnectionStatusLogout);

//            int protoStatus = ProtoLogic.getConnectionStatus();
//            if (mars::stn::getConnectionStatus() != mars::stn::kConnectionStatusConnected && mars::stn::getConnectionStatus() != mars::stn::kConnectionStatusReceiveing) {
//                [self destroyMars];
//            }

            ProtoLogic.disconnect(clearSession ? 8 : 0);

            resetProto();
        }

        @Override
        public void setForeground(int isForeground) throws RemoteException {
            BaseEvent.onForeground(isForeground == 1);
        }

        @Override
        public void onNetworkChange() {
            BaseEvent.onNetworkChange();
        }

        @Override
        public void setServerAddress(String host, int port) throws RemoteException {
            mHost = host;
            mPort = port;
        }

        @Override
        public void registerMessageContent(String msgContentCls) throws RemoteException {
            try {
                Class cls = Class.forName(msgContentCls);
                ContentTag tag = (ContentTag) cls.getAnnotation(ContentTag.class);
                if (tag != null) {
                    Class curClazz = contentMapper.get(tag.type());
                    if (curClazz != null && !curClazz.equals(cls)) {
                        throw new IllegalArgumentException("messageContent type duplicate");
                    }
                    contentMapper.put(tag.type(), cls);
                    ProtoLogic.registerMessageFlag(tag.type(), tag.flag().getValue());
                } else {
                    throw new IllegalStateException("ContentTag annotation must be set!");
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private ProtoMessage convertMessage(cn.wildfirechat.message.Message msg) {
            ProtoMessage protoMessage = new ProtoMessage();

            if (msg.conversation != null) {
                protoMessage.setConversationType(msg.conversation.type.ordinal());
                protoMessage.setTarget(msg.conversation.target);
                protoMessage.setLine(msg.conversation.line);
            }
            protoMessage.setFrom(msg.sender);
            protoMessage.setTos(msg.toUsers);
            MessagePayload payload = msg.content.encode();
            payload.contentType = msg.content.getClass().getAnnotation(ContentTag.class).type();
            protoMessage.setContent(payload.toProtoContent());
            protoMessage.setMessageId(msg.messageId);
            protoMessage.setDirection(msg.direction.ordinal());
            protoMessage.setStatus(msg.status.ordinal());
            protoMessage.setMessageUid(msg.messageUid);
            protoMessage.setTimestamp(msg.serverTime);

            return protoMessage;
        }

        @Override
        public void send(cn.wildfirechat.message.Message msg, final ISendMessageCallback callback, int expireDuration) throws RemoteException {

            msg.sender = userId;
            ProtoMessage protoMessage = convertMessage(msg);

            ProtoLogic.sendMessage(protoMessage, expireDuration, new ProtoLogic.ISendMessageCallback() {
                @Override
                public void onSuccess(long messageUid, long timestamp) {
                    try {
                        callback.onSuccess(messageUid, timestamp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    try {
                        callback.onFailure(errorCode);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onPrepared(long messageId, long savedTime) {
                    try {
                        callback.onPrepared(messageId, savedTime);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onProgress(long uploaded, long total) {
                    try {
                        callback.onProgress(uploaded, total);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMediaUploaded(String remoteUrl) {
                    try {
                        callback.onMediaUploaded(remoteUrl);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void recall(long messageUid, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.recallMessage(messageUid, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public long getServerDeltaTime() throws RemoteException {
            return ProtoLogic.getServerDeltaTime();
        }


        private ConversationInfo convertProtoConversationInfo(ProtoConversationInfo protoInfo) {
            if (protoInfo.getTarget() == null || protoInfo.getTarget().length() == 0) {
                return null;
            }
            ConversationInfo info = new ConversationInfo();
            info.conversation = new Conversation(Conversation.ConversationType.values()[protoInfo.getConversationType()], protoInfo.getTarget(), protoInfo.getLine());
            info.lastMessage = convertProtoMessage(protoInfo.getLastMessage());
            info.timestamp = protoInfo.getTimestamp();
            info.draft = protoInfo.getDraft();
            info.unreadCount = new UnreadCount(protoInfo.getUnreadCount());
            info.isTop = protoInfo.isTop();
            info.isSilent = protoInfo.isSilent();
            return info;
        }

        @Override
        public List<ConversationInfo> getConversationList(int[] conversationTypes, int[] lines) throws RemoteException {
            ProtoConversationInfo[] protoConversationInfos = ProtoLogic.getConversations(conversationTypes, lines);
            List<ConversationInfo> out = new ArrayList<>();
            for (ProtoConversationInfo protoConversationInfo : protoConversationInfos) {
                ConversationInfo info = convertProtoConversationInfo(protoConversationInfo);
                if (info != null)
                    out.add(info);
            }
            return out;
        }

        @Override
        public ConversationInfo getConversation(int conversationType, String target, int line) throws RemoteException {
            return convertProtoConversationInfo(ProtoLogic.getConversation(conversationType, target, line));
        }

        @Override
        public List<cn.wildfirechat.message.Message> getMessages(Conversation conversation, long fromIndex, boolean before, int count, String withUser) throws RemoteException {
            ProtoMessage[] protoMessages = ProtoLogic.getMessages(conversation.type.ordinal(), conversation.target, conversation.line, fromIndex, before, count, withUser);
            List<cn.wildfirechat.message.Message> out = new ArrayList<>();
            for (ProtoMessage protoMessage : protoMessages) {
                cn.wildfirechat.message.Message msg = convertProtoMessage(protoMessage);
                if (msg != null) {
                    out.add(msg);
                }
            }
            return out;
        }

        @Override
        public List<Message> getMessagesEx(int[] conversationTypes, int[] lines, int[] contentTypes, long fromIndex, boolean before, int count, String withUser) throws RemoteException {
            ProtoMessage[] protoMessages = ProtoLogic.getMessagesEx(conversationTypes, lines, contentTypes, fromIndex, before, count, withUser);
            List<cn.wildfirechat.message.Message> out = new ArrayList<>();
            for (ProtoMessage protoMessage : protoMessages) {
                cn.wildfirechat.message.Message msg = convertProtoMessage(protoMessage);
                if (msg != null) {
                    out.add(msg);
                }
            }
            return out;
        }

        @Override
        public List<Message> getMessagesEx2(int[] conversationTypes, int[] lines, int messageStatus, long fromIndex, boolean before, int count, String withUser) throws RemoteException {
            ProtoMessage[] protoMessages = ProtoLogic.getMessagesEx2(conversationTypes, lines, messageStatus, fromIndex, before, count, withUser);
            List<cn.wildfirechat.message.Message> out = new ArrayList<>();
            for (ProtoMessage protoMessage : protoMessages) {
                cn.wildfirechat.message.Message msg = convertProtoMessage(protoMessage);
                if (msg != null) {
                    out.add(msg);
                }
            }
            return out;
        }

        @Override
        public void getRemoteMessages(Conversation conversation, long beforeMessageUid, int count, IGetRemoteMessageCallback callback) throws RemoteException {
            ProtoLogic.getRemoteMessages(conversation.type.ordinal(), conversation.target, conversation.line, beforeMessageUid, count, new ProtoLogic.ILoadRemoteMessagesCallback() {
                @Override
                public void onSuccess(ProtoMessage[] list) {
                    List<cn.wildfirechat.message.Message> out = new ArrayList<>();
                    for (ProtoMessage protoMessage : list) {
                        cn.wildfirechat.message.Message msg = convertProtoMessage(protoMessage);
                        if (msg != null) {
                            out.add(msg);
                        }
                    }
                    try {
                        callback.onSuccess(out);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public cn.wildfirechat.message.Message getMessage(long messageId) throws RemoteException {
            return convertProtoMessage(ProtoLogic.getMessage(messageId));
        }

        @Override
        public cn.wildfirechat.message.Message getMessageByUid(long messageUid) throws RemoteException {
            return convertProtoMessage(ProtoLogic.getMessageByUid(messageUid));
        }

        @Override
        public cn.wildfirechat.message.Message insertMessage(cn.wildfirechat.message.Message message, boolean notify) throws RemoteException {
            ProtoMessage protoMessage = convertMessage(message);
            message.messageId = ProtoLogic.insertMessage(protoMessage);
            return message;
        }

        @Override
        public boolean updateMessage(cn.wildfirechat.message.Message message) throws RemoteException {
            ProtoMessage protoMessage = convertMessage(message);
            ProtoLogic.updateMessageContent(protoMessage);
            return false;
        }

        @Override
        public UnreadCount getUnreadCount(int conversationType, String target, int line) throws RemoteException {
            return new UnreadCount(ProtoLogic.getUnreadCount(conversationType, target, line));
        }

        @Override
        public UnreadCount getUnreadCountEx(int[] conversationTypes, int[] lines) throws RemoteException {
            return new UnreadCount(ProtoLogic.getUnreadCountEx(conversationTypes, lines));
        }

        @Override
        public void clearUnreadStatus(int conversationType, String target, int line) throws RemoteException {
            ProtoLogic.clearUnreadStatus(conversationType, target, line);
        }

        @Override
        public void clearUnreadStatusEx(int[] conversationTypes, int[] lines) throws RemoteException {
            ProtoLogic.clearUnreadStatusEx(conversationTypes, lines);
        }

        @Override
        public void clearAllUnreadStatus() throws RemoteException {
            ProtoLogic.clearAllUnreadStatus();
        }

        @Override
        public void clearMessages(int conversationType, String target, int line) throws RemoteException {
            ProtoLogic.clearMessages(conversationType, target, line);
        }

        @Override
        public void setMediaMessagePlayed(long messageId) {
            try {
                Message message = getMessage(messageId);
                if (message == null || message.direction == MessageDirection.Send || !(message.content instanceof MediaMessageContent)) {
                    return;
                }
                ProtoLogic.setMediaMessagePlayed(messageId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void removeConversation(int conversationType, String target, int line, boolean clearMsg) throws RemoteException {
            ProtoLogic.removeConversation(conversationType, target, line, clearMsg);
        }

        @Override
        public void setConversationTop(int conversationType, String target, int line, boolean top) throws RemoteException {
            setUserSetting(ConversationTop, conversationType + "-" + line + "-" + target, top ? "1" : "0", null);
        }

        @Override
        public void setConversationDraft(int conversationType, String target, int line, String draft) throws RemoteException {
            ProtoLogic.setConversationDraft(conversationType, target, line, draft);
        }

        @Override
        public void setConversationSilent(int conversationType, String target, int line, boolean silent) throws RemoteException {
            setUserSetting(ConversationSilent, conversationType + "-" + line + "-" + target, silent ? "1" : "0", null);
        }

        @Override
        public void searchUser(String keyword, boolean fuzzy, final ISearchUserCallback callback) throws RemoteException {
            ProtoLogic.searchUser(keyword, fuzzy, 0, new ProtoLogic.ISearchUserCallback() {
                @Override
                public void onSuccess(ProtoUserInfo[] userInfos) {
                    List<UserInfo> out = new ArrayList<>();
                    if (userInfos != null) {
                        for (ProtoUserInfo protoUserInfo : userInfos) {
                            out.add(convertProtoUserInfo(protoUserInfo));
                        }
                    }
                    try {
                        callback.onSuccess(out);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    try {
                        callback.onFailure(errorCode);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public boolean isMyFriend(String userId) throws RemoteException {
            return ProtoLogic.isMyFriend(userId);
        }

        @Override
        public List<String> getMyFriendList(boolean refresh) throws RemoteException {
            List<String> out = new ArrayList<>();
            String[] friends = ProtoLogic.getMyFriendList(refresh);
            if (friends != null) {
                for (String friend : friends) {
                    out.add(friend);
                }
            }
            return out;
        }

        @Override
        public boolean isBlackListed(String userId) throws RemoteException {
            return ProtoLogic.isBlackListed(userId);
        }

        @Override
        public List<String> getBlackList(boolean refresh) throws RemoteException {
            List<String> out = new ArrayList<>();
            String[] friends = ProtoLogic.getBlackList(refresh);
            if (friends != null) {
                for (String friend : friends) {
                    out.add(friend);
                }
            }
            return out;
        }

        @Override
        public List<UserInfo> getMyFriendListInfo(boolean refresh) throws RemoteException {
            List<String> users = getMyFriendList(refresh);
            List<UserInfo> userInfos = new ArrayList<>();
            UserInfo userInfo;
            for (String user : users) {
                userInfo = getUserInfo(user, null, false);
                if (userInfo == null) {
                    userInfo = new UserInfo();
                    userInfo.uid = user;
                }
                userInfos.add(userInfo);
            }
            return userInfos;
        }

        @Override
        public void loadFriendRequestFromRemote() throws RemoteException {
            ProtoLogic.loadFriendRequestFromRemote();
        }

        @Override
        public String getUserSetting(int scope, String key) throws RemoteException {
            return ProtoLogic.getUserSetting(scope, key);
        }

        @Override
        public Map<String, String> getUserSettings(int scope) throws RemoteException {
            return ProtoLogic.getUserSettings(scope);
        }

        @Override
        public void setUserSetting(int scope, String key, String value, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.setUserSetting(scope, key, value, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        if (callback != null)
                            callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        if (callback != null) {
                            callback.onFailure(i);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void startLog() throws RemoteException {
            Xlog.setConsoleLogOpen(true);
        }

        @Override
        public void stopLog() throws RemoteException {
            Xlog.setConsoleLogOpen(false);
        }

        @Override
        public void setDeviceToken(String token, int pushType) throws RemoteException {
            if (TextUtils.isEmpty(token)) {
                return;
            }
            mBackupDeviceToken = token;
            mBackupPushType = pushType;
            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("mars_core_push_type", pushType).commit();
            if (mConnectionStatus != ConnectionStatusConnected) {
                return;
            }

            ProtoLogic.setDeviceToken(getApplicationContext().getPackageName(), token, pushType);
            mBackupDeviceToken = null;
        }

        private FriendRequest convertProtoFriendRequest(ProtoFriendRequest protoRequest) {
            FriendRequest request = new FriendRequest();

            request.direction = protoRequest.getDirection();
            request.target = protoRequest.getTarget();
            request.reason = protoRequest.getReason();
            request.status = protoRequest.getStatus();
            request.readStatus = protoRequest.getReadStatus();
            request.timestamp = protoRequest.getTimestamp();

            return request;
        }

        @Override
        public List<FriendRequest> getFriendRequest(boolean incomming) throws RemoteException {
            List<FriendRequest> out = new ArrayList<>();
            ProtoFriendRequest[] requests = ProtoLogic.getFriendRequest(incomming);
            if (requests != null) {
                for (ProtoFriendRequest protoFriendRequest : requests) {
                    out.add(convertProtoFriendRequest(protoFriendRequest));
                }
            }
            return out;
        }

        @Override
        public String getFriendAlias(String userId) throws RemoteException {
            return ProtoLogic.getFriendAlias(userId);
        }

        @Override
        public void setFriendAlias(String userId, String alias, IGeneralCallback callback) throws RemoteException {
            ProtoLogic.setFriendAlias(userId, alias, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) {
                        try {
                            callback.onSuccess();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(int i) {
                    if (callback != null) {
                        try {
                            callback.onFailure(i);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        @Override
        public void clearUnreadFriendRequestStatus() throws RemoteException {
            ProtoLogic.clearUnreadFriendRequestStatus();
        }

        @Override
        public int getUnreadFriendRequestStatus() throws RemoteException {
            return ProtoLogic.getUnreadFriendRequestStatus();
        }

        @Override
        public void removeFriend(String userId, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.removeFriend(userId, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    try {
                        callback.onFailure(errorCode);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void sendFriendRequest(String userId, String reason, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.sendFriendRequest(userId, reason, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    try {
                        callback.onFailure(errorCode);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void handleFriendRequest(String userId, boolean accept, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.handleFriendRequest(userId, accept, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    try {
                        callback.onFailure(errorCode);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void setBlackList(String userId, boolean isBlacked, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.setBlackList(userId, isBlacked, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    try {
                        callback.onFailure(errorCode);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void joinChatRoom(String chatRoomId, IGeneralCallback callback) throws RemoteException {
            ProtoLogic.joinChatRoom(chatRoomId, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void quitChatRoom(String chatRoomId, IGeneralCallback callback) throws RemoteException {
            ProtoLogic.quitChatRoom(chatRoomId, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

        }

        @Override
        public void getChatRoomInfo(String chatRoomId, long updateDt, IGetChatRoomInfoCallback callback) throws RemoteException {
            ProtoLogic.getChatRoomInfo(chatRoomId, updateDt, new ProtoLogic.IGetChatRoomInfoCallback() {

                @Override
                public void onSuccess(ProtoChatRoomInfo protoChatRoomInfo) {
                    try {
                        callback.onSuccess(converProtoChatRoomInfo(protoChatRoomInfo));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void getChatRoomMembersInfo(String chatRoomId, int maxCount, IGetChatRoomMembersInfoCallback callback) throws RemoteException {
            ProtoLogic.getChatRoomMembersInfo(chatRoomId, maxCount, new ProtoLogic.IGetChatRoomMembersInfoCallback() {
                @Override
                public void onSuccess(ProtoChatRoomMembersInfo protoChatRoomMembersInfo) {
                    try {
                        callback.onSuccess(convertProtoChatRoomMembersInfo(protoChatRoomMembersInfo));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }


        @Override
        public void deleteFriend(String userId, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.deleteFriend(userId, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    try {
                        callback.onFailure(errorCode);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public GroupInfo getGroupInfo(String groupId, boolean refresh) throws RemoteException {
            ProtoGroupInfo protoGroupInfo = ProtoLogic.getGroupInfo(groupId, refresh);
            return convertProtoGroupInfo(protoGroupInfo);
        }

        @Override
        public UserInfo getUserInfo(String userId, String groupId, boolean refresh) throws RemoteException {
            return convertProtoUserInfo(ProtoLogic.getUserInfo(userId, groupId == null ? "" : groupId, refresh));
        }

        @Override
        public List<UserInfo> getUserInfos(List<String> userIds, String groupId) throws RemoteException {
            List<UserInfo> userInfos = new ArrayList<>();
            String[] userIdsArray = new String[userIds.size()];
            ProtoUserInfo[] protoUserInfos = ProtoLogic.getUserInfos(userIds.toArray(userIdsArray), groupId == null ? "" : groupId);

            for (ProtoUserInfo protoUserInfo : protoUserInfos) {
                UserInfo userInfo = convertProtoUserInfo(protoUserInfo);
                if (userInfo.name == null && userInfo.displayName == null) {
                    userInfo = new NullUserInfo(userInfo.uid);
                }
                userInfos.add(userInfo);
            }
            return userInfos;
        }

        @Override
        public void uploadMedia(byte[] data, int mediaType, final IUploadMediaCallback callback) throws RemoteException {
            ProtoLogic.uploadMedia(data, mediaType, new ProtoLogic.IUploadMediaCallback() {
                @Override
                public void onSuccess(String s) {
                    try {
                        callback.onSuccess(s);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onProgress(long uploaded, long total) {

                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void modifyMyInfo(List<ModifyMyInfoEntry> values, final IGeneralCallback callback) throws RemoteException {
            Map<Integer, String> protoValues = new HashMap<>();
            for (ModifyMyInfoEntry entry : values
            ) {
                protoValues.put(entry.type.getValue(), entry.value);
            }
            ProtoLogic.modifyMyInfo(protoValues, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public boolean deleteMessage(long messageId) throws RemoteException {
            return ProtoLogic.deleteMessage(messageId);
        }

        @Override
        public List<ConversationSearchResult> searchConversation(String keyword, int[] conversationTypes, int[] lines) throws RemoteException {
            ProtoConversationSearchresult[] protoResults = ProtoLogic.searchConversation(keyword, conversationTypes, lines);
            List<ConversationSearchResult> output = new ArrayList<>();
            if (protoResults != null) {
                for (ProtoConversationSearchresult protoResult : protoResults
                ) {
                    ConversationSearchResult result = new ConversationSearchResult();
                    result.conversation = new Conversation(Conversation.ConversationType.type(protoResult.getConversationType()), protoResult.getTarget(), protoResult.getLine());
                    result.marchedMessage = convertProtoMessage(protoResult.getMarchedMessage());
                    result.timestamp = protoResult.getTimestamp();
                    result.marchedCount = protoResult.getMarchedCount();
                    output.add(result);

                }
            }

            return output;
        }

        @Override
        public List<cn.wildfirechat.message.Message> searchMessage(Conversation conversation, String keyword) throws RemoteException {
            ProtoMessage[] protoMessages = ProtoLogic.searchMessage(conversation.type.getValue(), conversation.target, conversation.line, keyword);
            List<cn.wildfirechat.message.Message> out = new ArrayList<>();

            if (protoMessages != null) {
                for (ProtoMessage protoMsg : protoMessages) {
                    Message msg = convertProtoMessage(protoMsg);
                    if (msg != null) {
                        out.add(convertProtoMessage(protoMsg));
                    }
                }
            }

            return out;
        }


        @Override
        public List<GroupSearchResult> searchGroups(String keyword) throws RemoteException {
            ProtoGroupSearchResult[] protoResults = ProtoLogic.searchGroups(keyword);
            List<GroupSearchResult> output = new ArrayList<>();
            if (protoResults != null) {
                for (ProtoGroupSearchResult protoResult : protoResults
                ) {
                    GroupSearchResult result = new GroupSearchResult();
                    result.groupInfo = convertProtoGroupInfo(protoResult.getGroupInfo());
                    result.marchedType = protoResult.getMarchType();
                    result.marchedMembers = new ArrayList<String>(Arrays.asList(protoResult.getMarchedMembers()));
                    output.add(result);
                }
            }

            return output;
        }

        @Override
        public List<UserInfo> searchFriends(String keyworkd) throws RemoteException {
            ProtoUserInfo[] protoUserInfos = ProtoLogic.searchFriends(keyworkd);
            List<UserInfo> out = new ArrayList<>();
            if (protoUserInfos != null) {
                for (ProtoUserInfo protoUserInfo : protoUserInfos) {
                    out.add(convertProtoUserInfo(protoUserInfo));
                }
            }
            return out;
        }

        @Override
        public String getEncodedClientId() throws RemoteException {
            return StnLogic.clientId();
        }

        @Override
        public void createGroup(String groupId, String groupName, String groupPortrait, int groupType, List<String> memberIds, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback2 callback) throws RemoteException {
            String[] memberArray = new String[memberIds.size()];
            for (int i = 0; i < memberIds.size(); i++) {
                memberArray[i] = memberIds.get(i);
            }
            ProtoLogic.createGroup(groupId, groupName, groupPortrait, groupType, memberArray, notifyLines, notifyMsg == null ? null : notifyMsg.toProtoContent(), new ProtoLogic.IGeneralCallback2() {
                @Override
                public void onSuccess(String s) {
                    try {
                        callback.onSuccess(s);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void addGroupMembers(String groupId, List<String> memberIds, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            String[] memberArray = new String[memberIds.size()];
            for (int i = 0; i < memberIds.size(); i++) {
                memberArray[i] = memberIds.get(i);
            }
            ProtoLogic.addMembers(groupId, memberArray, notifyLines, notifyMsg == null ? null : notifyMsg.toProtoContent(), new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void removeGroupMembers(String groupId, List<String> memberIds, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            String[] memberArray = new String[memberIds.size()];
            for (int i = 0; i < memberIds.size(); i++) {
                memberArray[i] = memberIds.get(i);
            }
            ProtoLogic.kickoffMembers(groupId, memberArray, notifyLines, notifyMsg == null ? null : notifyMsg.toProtoContent(), new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void quitGroup(String groupId, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.quitGroup(groupId, notifyLines, notifyMsg == null ? null : notifyMsg.toProtoContent(), new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void dismissGroup(String groupId, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.dismissGroup(groupId, notifyLines, notifyMsg == null ? null : notifyMsg.toProtoContent(), new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    // side
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void modifyGroupInfo(String groupId, int modifyType, String newValue, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.modifyGroupInfo(groupId, modifyType, newValue, notifyLines, notifyMsg == null ? null : notifyMsg.toProtoContent(), new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void modifyGroupAlias(String groupId, String newAlias, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.modifyGroupAlias(groupId, newAlias, notifyLines, notifyMsg == null ? null : notifyMsg.toProtoContent(), new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public List<GroupMember> getGroupMembers(String groupId, boolean forceUpdate) throws RemoteException {
            ProtoGroupMember[] protoGroupMembers = ProtoLogic.getGroupMembers(groupId, forceUpdate);
            List<GroupMember> out = new ArrayList<>();
            for (ProtoGroupMember protoMember : protoGroupMembers) {
                if (protoMember != null && !TextUtils.isEmpty(protoMember.getMemberId())) {
                    GroupMember member = new GroupMember();
                    member.groupId = groupId;
                    member.memberId = protoMember.getMemberId();
                    member.alias = protoMember.getAlias();
                    member.type = GroupMember.GroupMemberType.type(protoMember.getType());
                    member.updateDt = protoMember.getUpdateDt();

                    out.add(member);
                }
            }
            return out;
        }

        @Override
        public GroupMember getGroupMember(String groupId, String memberId) throws RemoteException {
            ProtoGroupMember protoGroupMember = ProtoLogic.getGroupMember(groupId, memberId);
            if (protoGroupMember == null || TextUtils.isEmpty(protoGroupMember.getMemberId())) {
                return new NullGroupMember(groupId, memberId);
            } else {
                return covertProtoGroupMember(protoGroupMember);
            }
        }

        @Override
        public void transferGroup(String groupId, String newOwner, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            ProtoLogic.transferGroup(groupId, newOwner, notifyLines, notifyMsg == null ? null : notifyMsg.toProtoContent(), new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void setGroupManager(String groupId, boolean isSet, List<String> memberIds, int[] notifyLines, MessagePayload notifyMsg, IGeneralCallback callback) throws RemoteException {
            String[] memberArray = new String[memberIds.size()];
            for (int i = 0; i < memberIds.size(); i++) {
                memberArray[i] = memberIds.get(i);
            }
            ProtoLogic.setGroupManager(groupId, isSet, memberArray, notifyLines, notifyMsg == null ? null : notifyMsg.toProtoContent(), new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public byte[] encodeData(byte[] data) throws RemoteException {
            return StnLogic.encodeData(data);
        }

        @Override
        public byte[] decodeData(byte[] data) throws RemoteException {
            return StnLogic.decodeData(data);
        }


        @Override
        public void createChannel(String channelId, String channelName, String channelPortrait, String desc, String extra, ICreateChannelCallback callback) throws RemoteException {
            ProtoLogic.createChannel(channelId, channelName, channelPortrait, 0, desc, extra, new ProtoLogic.ICreateChannelCallback() {
                @Override
                public void onSuccess(ProtoChannelInfo protoChannelInfo) {
                    try {
                        callback.onSuccess(converProtoChannelInfo(protoChannelInfo));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void modifyChannelInfo(String channelId, int modifyType, String newValue, IGeneralCallback callback) throws RemoteException {

        }

        @Override
        public ChannelInfo getChannelInfo(String channelId, boolean refresh) throws RemoteException {
            return converProtoChannelInfo(ProtoLogic.getChannelInfo(channelId, refresh));
        }

        @Override
        public void searchChannel(String keyword, ISearchChannelCallback callback) throws RemoteException {
            ProtoLogic.searchChannel(keyword, new ProtoLogic.ISearchChannelCallback() {
                @Override
                public void onSuccess(ProtoChannelInfo[] protoChannelInfos) {
                    List<ChannelInfo> out = new ArrayList<>();
                    if (protoChannelInfos != null) {
                        for (ProtoChannelInfo protoChannelInfo : protoChannelInfos) {
                            out.add(converProtoChannelInfo(protoChannelInfo));
                        }
                    }
                    try {
                        callback.onSuccess(out);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public boolean isListenedChannel(String channelId) throws RemoteException {
            return ProtoLogic.isListenedChannel(channelId);
        }

        @Override
        public void listenChannel(String channelId, boolean listen, IGeneralCallback callback) throws RemoteException {
            ProtoLogic.listenChannel(channelId, listen, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void destoryChannel(String channelId, IGeneralCallback callback) throws RemoteException {
            ProtoLogic.destoryChannel(channelId, new ProtoLogic.IGeneralCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int i) {
                    try {
                        callback.onFailure(i);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public List<String> getMyChannels() throws RemoteException {
            List<String> out = new ArrayList<>();
            String[] channels = ProtoLogic.getMyChannels();
            if (channels != null) {
                for (String channelId : channels) {
                    out.add(channelId);
                }
            }
            return out;
        }

        @Override
        public List<String> getListenedChannels() throws RemoteException {
            List<String> out = new ArrayList<>();
            String[] channels = ProtoLogic.getListenedChannels();
            if (channels != null) {
                for (String channelId : channels) {
                    out.add(channelId);
                }
            }
            return out;
        }

    }

    private ChannelInfo converProtoChannelInfo(ProtoChannelInfo protoChannelInfo) {
        if (protoChannelInfo == null) {
            return null;
        }
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.channelId = protoChannelInfo.getChannelId();
        channelInfo.name = protoChannelInfo.getName();
        channelInfo.desc = protoChannelInfo.getDesc();
        channelInfo.portrait = protoChannelInfo.getPortrait();
        channelInfo.extra = protoChannelInfo.getExtra();
        channelInfo.owner = protoChannelInfo.getOwner();
        channelInfo.status = ChannelInfo.ChannelStatus.status(protoChannelInfo.getStatus());
        channelInfo.updateDt = protoChannelInfo.getUpdateDt();

        return channelInfo;
    }

    private GroupInfo convertProtoGroupInfo(ProtoGroupInfo protoGroupInfo) {
        if (protoGroupInfo == null) {
            return null;
        }
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.target = protoGroupInfo.getTarget();
        groupInfo.name = protoGroupInfo.getName();
        groupInfo.portrait = protoGroupInfo.getPortrait();
        groupInfo.owner = protoGroupInfo.getOwner();
        groupInfo.type = GroupInfo.GroupType.type(protoGroupInfo.getType());
        groupInfo.memberCount = protoGroupInfo.getMemberCount();
        groupInfo.extra = protoGroupInfo.getExtra();
        groupInfo.updateDt = protoGroupInfo.getUpdateDt();
        groupInfo.mute = protoGroupInfo.getMute();
        groupInfo.joinType = protoGroupInfo.getJoinType();
        groupInfo.privateChat = protoGroupInfo.getPrivateChat();
        groupInfo.searchable = protoGroupInfo.getSearchable();
        return groupInfo;
    }

    private GroupMember covertProtoGroupMember(ProtoGroupMember protoGroupMember) {
        if (protoGroupMember == null) {
            return null;
        }
        GroupMember member = new GroupMember();
        member.groupId = protoGroupMember.getGroupId();
        member.memberId = protoGroupMember.getMemberId();
        member.alias = protoGroupMember.getAlias();
        member.type = GroupMember.GroupMemberType.type(protoGroupMember.getType());
        member.updateDt = protoGroupMember.getUpdateDt();
        return member;

    }

    private ChatRoomInfo converProtoChatRoomInfo(ProtoChatRoomInfo protoChatRoomInfo) {
        if (protoChatRoomInfo == null) {
            return null;
        }
        ChatRoomInfo chatRoomInfo = new ChatRoomInfo();
        chatRoomInfo.chatRoomId = protoChatRoomInfo.getChatRoomId();
        chatRoomInfo.title = protoChatRoomInfo.getTitle();
        chatRoomInfo.desc = protoChatRoomInfo.getDesc();
        chatRoomInfo.portrait = protoChatRoomInfo.getPortrait();
        chatRoomInfo.extra = protoChatRoomInfo.getExtra();
        chatRoomInfo.state = ChatRoomInfo.State.values()[protoChatRoomInfo.getState()];
        chatRoomInfo.memberCount = protoChatRoomInfo.getMemberCount();
        chatRoomInfo.createDt = protoChatRoomInfo.getCreateDt();
        chatRoomInfo.updateDt = protoChatRoomInfo.getUpdateDt();

        return chatRoomInfo;
    }

    private ChatRoomMembersInfo convertProtoChatRoomMembersInfo(ProtoChatRoomMembersInfo protoChatRoomMembersInfo) {
        //public int memberCount;
        //public List<String> members;
        if (protoChatRoomMembersInfo == null) {
            return null;
        }
        ChatRoomMembersInfo chatRoomMembersInfo = new ChatRoomMembersInfo();
        chatRoomMembersInfo.memberCount = protoChatRoomMembersInfo.getMemberCount();
        chatRoomMembersInfo.members = protoChatRoomMembersInfo.getMembers();
        return chatRoomMembersInfo;
    }


    private UserInfo convertProtoUserInfo(ProtoUserInfo protoUserInfo) {
        if (protoUserInfo == null) {
            return null;
        }
        UserInfo userInfo = new UserInfo();
        userInfo.uid = protoUserInfo.getUid();
        userInfo.name = protoUserInfo.getName();
        userInfo.displayName = protoUserInfo.getDisplayName();
        userInfo.portrait = protoUserInfo.getPortrait();
        userInfo.gender = protoUserInfo.getGender();
        userInfo.mobile = protoUserInfo.getMobile();
        userInfo.email = protoUserInfo.getEmail();
        userInfo.address = protoUserInfo.getAddress();
        userInfo.company = protoUserInfo.getCompany();
        userInfo.social = protoUserInfo.getSocial();
        userInfo.extra = protoUserInfo.getExtra();
        userInfo.updateDt = protoUserInfo.getUpdateDt();
        userInfo.type = protoUserInfo.getType();
        userInfo.friendAlias = protoUserInfo.getFriendAlias();
        userInfo.groupAlias = protoUserInfo.getGroupAlias();
        return userInfo;
    }

    private MessageContent contentOfType(int type) {
        Class<? extends MessageContent> cls = contentMapper.get(type);
        if (cls != null) {
            try {
                return cls.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return new UnknownMessageContent();
    }

    private final ClientServiceStub mBinder = new ClientServiceStub();

    private List<cn.wildfirechat.message.Message> convertProtoMessages(List<ProtoMessage> protoMessages) {
        List<cn.wildfirechat.message.Message> out = new ArrayList<>();
        for (ProtoMessage protoMessage : protoMessages) {
            cn.wildfirechat.message.Message msg = convertProtoMessage(protoMessage);
            if (msg != null && msg.content != null) {
                out.add(msg);
            }
        }
        return out;
    }

    private cn.wildfirechat.message.Message convertProtoMessage(ProtoMessage protoMessage) {
        if (protoMessage == null || TextUtils.isEmpty(protoMessage.getTarget())) {
            return null;
        }
        cn.wildfirechat.message.Message msg = new cn.wildfirechat.message.Message();
        msg.messageId = protoMessage.getMessageId();
        msg.conversation = new Conversation(Conversation.ConversationType.values()[protoMessage.getConversationType()], protoMessage.getTarget(), protoMessage.getLine());
        msg.sender = protoMessage.getFrom();
        msg.toUsers = protoMessage.getTos();

        msg.content = contentOfType(protoMessage.getContent().getType());
        MessagePayload payload = new MessagePayload(protoMessage.getContent());
        try {
            msg.content.decode(payload);
            if (msg.content instanceof NotificationMessageContent) {
                if (msg.sender.equals(userId)) {
                    ((NotificationMessageContent) msg.content).fromSelf = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (msg.content.getPersistFlag() == PersistFlag.Persist || msg.content.getPersistFlag() == PersistFlag.Persist_And_Count) {
                msg.content = new UnknownMessageContent();
                ((UnknownMessageContent) msg.content).setOrignalPayload(payload);
            } else {
                return null;
            }
        }

        msg.direction = MessageDirection.values()[protoMessage.getDirection()];
        msg.status = MessageStatus.values()[protoMessage.getStatus()];
        msg.messageUid = protoMessage.getMessageUid();
        msg.serverTime = protoMessage.getTimestamp();

        return msg;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Mars PlatformComm
        handler = new Handler(Looper.getMainLooper());
        Mars.init(getApplicationContext(), handler);
        if (mConnectionReceiver == null) {
            mConnectionReceiver = new BaseEvent.ConnectionReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(mConnectionReceiver, filter);
        }

        try {
            mBinder.registerMessageContent(AddGroupMemberNotificationContent.class.getName());
            mBinder.registerMessageContent(CallStartMessageContent.class.getName());
            mBinder.registerMessageContent(ChangeGroupNameNotificationContent.class.getName());
            mBinder.registerMessageContent(ChangeGroupPortraitNotificationContent.class.getName());
            mBinder.registerMessageContent(CreateGroupNotificationContent.class.getName());
            mBinder.registerMessageContent(DismissGroupNotificationContent.class.getName());
            mBinder.registerMessageContent(FileMessageContent.class.getName());
            mBinder.registerMessageContent(ImageMessageContent.class.getName());
            mBinder.registerMessageContent(ImageTextMessageContent.class.getName());
            mBinder.registerMessageContent(KickoffGroupMemberNotificationContent.class.getName());
            mBinder.registerMessageContent(LocationMessageContent.class.getName());
            mBinder.registerMessageContent(ModifyGroupAliasNotificationContent.class.getName());
            mBinder.registerMessageContent(QuitGroupNotificationContent.class.getName());
            mBinder.registerMessageContent(RecallMessageContent.class.getName());
            mBinder.registerMessageContent(SoundMessageContent.class.getName());
            mBinder.registerMessageContent(StickerMessageContent.class.getName());
            mBinder.registerMessageContent(TextMessageContent.class.getName());
            mBinder.registerMessageContent(TipNotificationContent.class.getName());
            mBinder.registerMessageContent(TransferGroupOwnerNotificationContent.class.getName());
            mBinder.registerMessageContent(VideoMessageContent.class.getName());
            mBinder.registerMessageContent(TypingMessageContent.class.getName());
            mBinder.registerMessageContent(GroupMuteNotificationContent.class.getName());
            mBinder.registerMessageContent(GroupJoinTypeNotificationContent.class.getName());
            mBinder.registerMessageContent(GroupPrivateChatNotificationContent.class.getName());
            mBinder.registerMessageContent(GroupSetManagerChatNotificationContent.class.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Log.appenderClose();
        super.onDestroy();
        resetProto();
        if (mConnectionReceiver != null) {
            unregisterReceiver(mConnectionReceiver);
            mConnectionReceiver = null;
        }
    }

    private boolean initProto(String userName, String userPwd) {
        AppLogic.setCallBack(this);
        SdtLogic.setCallBack(this);

        Mars.onCreate(true);
        openXlog();

        mConnectionStatus = ConnectionStatusLogout;

        ProtoLogic.setUserInfoUpdateCallback(this);
        ProtoLogic.setSettingUpdateCallback(this);
        ProtoLogic.setFriendListUpdateCallback(this);
        ProtoLogic.setGroupInfoUpdateCallback(this);
        ProtoLogic.setChannelInfoUpdateCallback(this);
        ProtoLogic.setGroupMembersUpdateCallback(this);
        ProtoLogic.setFriendRequestListUpdateCallback(this);

        ProtoLogic.setConnectionStatusCallback(ClientService.this);
        ProtoLogic.setReceiveMessageCallback(ClientService.this);
        ProtoLogic.setAuthInfo(userName, userPwd);
        return ProtoLogic.connect(mHost, mPort);
    }

    private void resetProto() {
        Mars.onDestroy();
        AppLogic.setCallBack(null);
        SdtLogic.setCallBack(null);
        // Receiver may not registered
//        Alarm.resetAlarm(this);
        ProtoLogic.setUserInfoUpdateCallback(null);
        ProtoLogic.setSettingUpdateCallback(null);
        ProtoLogic.setFriendListUpdateCallback(null);
        ProtoLogic.setGroupInfoUpdateCallback(null);
        ProtoLogic.setChannelInfoUpdateCallback(null);
        ProtoLogic.setFriendRequestListUpdateCallback(null);

        ProtoLogic.setConnectionStatusCallback(null);
        ProtoLogic.setReceiveMessageCallback(null);
    }

    public void openXlog() {

//        int pid = android.os.Process.myPid();
        String processName = getApplicationInfo().packageName;
//        ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningAppProcessInfo appProcess : am.getRunningAppProcesses()) {
//            if (appProcess.pid == pid) {
//                processName = appProcess.processName;
//                break;
//            }
//        }

        if (processName == null) {
            return;
        }

        final String SDCARD;
        if (checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            SDCARD = getCacheDir().getAbsolutePath();
        } else {
            SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        final String logPath = SDCARD + "/marscore/log";
        final String logCache = SDCARD + "/marscore/cache";

        String logFileName = processName.indexOf(":") == -1 ? "MarsSample" : ("MarsSample_" + processName.substring(processName.indexOf(":") + 1));

        if (BuildConfig.DEBUG) {
            Xlog.appenderOpen(Xlog.LEVEL_VERBOSE, Xlog.AppednerModeAsync, logCache, logPath, logFileName, "");
            Xlog.setConsoleLogOpen(true);
        } else {
            Xlog.appenderOpen(Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, logCache, logPath, logFileName, "");
            Xlog.setConsoleLogOpen(false);
        }
        Log.setLogImp(new Xlog());
    }

    private class WfcRemoteCallbackList<E extends IInterface> extends RemoteCallbackList<E> {
        @Override
        public void onCallbackDied(E callback, Object cookie) {
            Log.e("ClientService", "main process died");
            Intent intent = new Intent(ClientService.this, RecoverReceiver.class);
            sendBroadcast(intent);
        }
    }

    @Override
    public void reportSignalDetectResults(String resultsJson) {

    }

    @Override
    public String getAppFilePath() {
        try {
            File file = new File(ClientService.this.getFilesDir().getAbsolutePath() + "/" + accountInfo.userName);
            if (!file.exists()) {
                file.mkdir();
            }
            return file.toString();
        } catch (Exception e) {
            Log.e("ddd", "", e);
        }

        return null;
    }

    @Override
    public AppLogic.AccountInfo getAccountInfo() {
        return accountInfo;
    }

    @Override
    public int getClientVersion() {
        return 0;
    }

    @Override
    public AppLogic.DeviceInfo getDeviceType() {
        if (info == null) {
            String imei = PreferenceManager.getDefaultSharedPreferences(context).getString("mars_core_uid", "");
            if (TextUtils.isEmpty(imei)) {
                imei = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                if (TextUtils.isEmpty(imei)) {
                    imei = UUID.randomUUID().toString();
                }
                imei += System.currentTimeMillis();
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString("mars_core_uid", imei).commit();
            }
            info = new AppLogic.DeviceInfo(imei);
            info.packagename = context.getPackageName();
            info.device = Build.MANUFACTURER;
            info.deviceversion = Build.VERSION.RELEASE;
            info.phonename = Build.MODEL;

            Locale locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locale = LocaleList.getDefault().get(0);
            } else {
                locale = Locale.getDefault();
            }

            info.language = locale.getLanguage();
        }
        return info;
    }

    @Override
    public void onConnectionStatusChanged(int status) {
        android.util.Log.d("", "status changed :" + status);
        mConnectionStatus = status;
        if (status == -4) {
            status = -1;
        }
        int finalStatus = status;
        handler.post(() -> {
            int i = onConnectionStatusChangeListenes.beginBroadcast();
            IOnConnectionStatusChangeListener listener;
            while (i > 0) {
                i--;
                listener = onConnectionStatusChangeListenes.getBroadcastItem(i);
                try {
                    listener.onConnectionStatusChange(finalStatus);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onConnectionStatusChangeListenes.finishBroadcast();
        });

        if (mConnectionStatus == ConnectionStatusConnected && !TextUtils.isEmpty(mBackupDeviceToken)) {
            try {
                ProtoLogic.setDeviceToken(getApplicationContext().getPackageName(), mBackupDeviceToken, mBackupPushType);
                mBackupDeviceToken = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRecallMessage(long messageUid) {
        handler.post(() -> {
            int receiverCount = onReceiveMessageListeners.beginBroadcast();
            IOnReceiveMessageListener listener;
            while (receiverCount > 0) {
                receiverCount--;
                listener = onReceiveMessageListeners.getBroadcastItem(receiverCount);
                try {
                    listener.onRecall(messageUid);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onReceiveMessageListeners.finishBroadcast();
        });
    }

    private void onReceiveMessageInternal(List<Message> messages, boolean hasMore) {
        int receiverCount = onReceiveMessageListeners.beginBroadcast();
        IOnReceiveMessageListener listener;
        while (receiverCount > 0) {
            receiverCount--;
            listener = onReceiveMessageListeners.getBroadcastItem(receiverCount);
            try {
                listener.onReceive(messages, hasMore);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        onReceiveMessageListeners.finishBroadcast();
    }

    @Override
    public void onReceiveMessage(List<ProtoMessage> messages, boolean hasMore) {
        if (messages.isEmpty()) {
            return;
        }
        handler.post(() -> {
            int totalLength = 0;
            int messageContentLength;
            int maxIpcMessageLength = 900 * 1024;

            List<Message> msgs = new ArrayList<>();
            for (ProtoMessage pmsg : messages) {
                messageContentLength = getProtoMessageLength(pmsg);
                if (messageContentLength > maxIpcMessageLength) {
                    android.util.Log.e("ClientService", "drop message, too large: " + pmsg.getMessageUid() + " " + messageContentLength);
                    continue;
                }
                totalLength += messageContentLength;
                if (totalLength >= maxIpcMessageLength) {
                    onReceiveMessageInternal(msgs, msgs.size() < messages.size());
                    totalLength = 0;
                    msgs = new ArrayList<>();
                } else {
                    msgs.add(convertProtoMessage(pmsg));
                }
            }
            if (!msgs.isEmpty()) {
                onReceiveMessageInternal(msgs, false);
            }
        });
    }

    @Override
    public void onFriendListUpdated(String[] friendList) {
        if (friendList == null || friendList.length == 0) {
            return;
        }
        handler.post(() -> {
            int i = onFriendUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnFriendUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onFriendUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onFriendListUpdated(Arrays.asList(friendList));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onFriendUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    @Override
    public void onFriendRequestUpdated() {
        handler.post(() -> {
            int i = onFriendUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnFriendUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onFriendUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onFriendRequestUpdated();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onFriendUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    @Override
    public void onGroupInfoUpdated(List<ProtoGroupInfo> list) {
        handler.post(() -> {
            ArrayList<GroupInfo> groups = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                GroupInfo gi = convertProtoGroupInfo(list.get(i));
                if (gi != null) {
                    groups.add(gi);
                }
            }
            int i = onGroupInfoUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnGroupInfoUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onGroupInfoUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onGroupInfoUpdated(groups);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onGroupInfoUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    @Override
    public void onGroupMembersUpdated(String groupId, List<ProtoGroupMember> members) {
        handler.post(() -> {
            ArrayList<GroupMember> groupMembers = new ArrayList<>();
            for (int i = 0; i < members.size(); i++) {
                GroupMember gm = covertProtoGroupMember(members.get(i));
                if (gm != null) {
                    groupMembers.add(gm);
                }
            }
            int i = onGroupMembersUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnGroupMembersUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onGroupMembersUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onGroupMembersUpdated(groupId, groupMembers);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onGroupMembersUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }


    @Override
    public void onChannelInfoUpdated(List<ProtoChannelInfo> list) {
        handler.post(() -> {
            ArrayList<ChannelInfo> channels = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                ChannelInfo gi = converProtoChannelInfo(list.get(i));
                if (gi != null) {
                    channels.add(gi);
                }
            }
            int i = onChannelInfoUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnChannelInfoUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onChannelInfoUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onChannelInfoUpdated(channels);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onChannelInfoUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    // 参数里面直接带上scope, key, value
    @Override
    public void onSettingUpdated() {
        handler.post(() -> {
            int i = onSettingUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnSettingUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onSettingUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onSettingUpdated();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onSettingUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    @Override
    public void onUserInfoUpdated(List<ProtoUserInfo> list) {
        handler.post(() -> {
            ArrayList<UserInfo> users = new ArrayList<>();
            for (int j = 0; j < list.size(); j++) {
                UserInfo userInfo = convertProtoUserInfo(list.get(j));
                if (userInfo != null) {
                    users.add(userInfo);
                }
            }
            int i = onUserInfoUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnUserInfoUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onUserInfoUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onUserInfoUpdated(users);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onUserInfoUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    // 只是大体大小
    private int getProtoMessageLength(ProtoMessage message) {
        int length = 0;
        ProtoMessageContent content = message.getContent();
        length += content.getBinaryContent() != null ? content.getBinaryContent().length : 0;
        length += content.getContent() != null ? content.getContent().length() : 0;
        length += content.getSearchableContent() != null ? content.getSearchableContent().length() : 0;
        length += content.getPushContent() != null ? content.getPushContent().length() : 0;
        length += content.getLocalMediaPath() != null ? content.getLocalMediaPath().length() : 0;
        length += content.getRemoteMediaUrl() != null ? content.getRemoteMediaUrl().length() : 0;
        length += content.getLocalContent() != null ? content.getLocalContent().length() : 0;
        return length;
    }
}
