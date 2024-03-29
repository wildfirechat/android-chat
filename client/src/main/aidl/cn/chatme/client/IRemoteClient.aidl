// IRemoteClient.aidl
package cn.chatme.client;

import cn.chatme.client.ISendMessageCallback;
import cn.chatme.client.ISearchUserCallback;
import cn.chatme.client.IGeneralCallback;
import cn.chatme.client.IGeneralCallbackInt;
import cn.chatme.client.IGeneralCallback2;
import cn.chatme.client.IGeneralCallback3;
import cn.chatme.client.IUploadMediaCallback;
import cn.chatme.client.IOnReceiveMessageListener;
import cn.chatme.client.IOnConnectionStatusChangeListener;
import cn.chatme.client.IOnConnectToServerListener;
import cn.chatme.client.IGetChatRoomInfoCallback;
import cn.chatme.client.IGetChatRoomMembersInfoCallback;
import cn.chatme.client.IGetGroupInfoCallback;
import cn.chatme.client.ICreateChannelCallback;
import cn.chatme.client.ISearchChannelCallback;
import cn.chatme.client.IGetRemoteMessagesCallback;
import cn.chatme.client.IGetFileRecordCallback;
import cn.chatme.client.IGetAuthorizedMediaUrlCallback;
import cn.chatme.client.IGetUploadUrlCallback;
import cn.chatme.client.ICreateSecretChatCallback;

import cn.chatme.client.IGetMessageCallback;
import cn.chatme.client.IGetUserCallback;
import cn.chatme.client.IGetGroupCallback;
import cn.chatme.client.IGetGroupMemberCallback;
import cn.chatme.client.IGetConversationListCallback;
import cn.chatme.client.IWatchUserOnlineStateCallback;

import cn.chatme.client.IOnFriendUpdateListener;
import cn.chatme.client.IOnGroupInfoUpdateListener;
import cn.chatme.client.IOnGroupMembersUpdateListener;
import cn.chatme.client.IOnSettingUpdateListener;
import cn.chatme.client.IGetGroupsCallback;
import cn.chatme.client.IOnUserInfoUpdateListener;
import cn.chatme.client.IOnChannelInfoUpdateListener;
import cn.chatme.client.IOnConferenceEventListener;
import cn.chatme.client.IOnUserOnlineEventListener;
import cn.chatme.client.IOnTrafficDataListener;
import cn.chatme.client.IOnSecretChatStateListener;
import cn.chatme.client.IOnSecretMessageBurnStateListener;

import cn.chatme.message.Message;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.model.UnreadCount;

import cn.chatme.model.Conversation;
import cn.chatme.model.ConversationSearchResult;
import cn.chatme.model.GroupSearchResult;
import cn.chatme.model.ModifyMyInfoEntry;
import cn.chatme.model.ConversationInfo;
import cn.chatme.model.FriendRequest;
import cn.chatme.model.Friend;
import cn.chatme.model.UserInfo;
import cn.chatme.model.GroupMember;
import cn.chatme.model.GroupInfo;
import cn.chatme.model.ChannelInfo;
import cn.chatme.model.SecretChatInfo;
import cn.chatme.model.BurnMessageInfo;
import cn.chatme.model.Socks5ProxyInfo;


import java.util.List;
import java.util.Map;

import android.os.ParcelFileDescriptor;


// Declare any non-default types here with import statements

interface IRemoteClient {
    long connect(in String userId, in String token);
    void disconnect(in boolean disablePush, in boolean clearSession);
    void setForeground(in int isForeground);
    void onNetworkChange();
    void setServerAddress(in String host);
    void setBackupAddressStrategy(in int strategy);
    void setBackupAddress(in String host, in int port);
    void setProtoUserAgent(in String userAgent);
    void addHttpHeader(in String header, in String value);
    void setLiteMode(in boolean isLiteMode);
    void setLowBPSMode(in boolean isLowBPSMode);
    int getConnectionStatus();

    oneway void setOnReceiveMessageListener(in IOnReceiveMessageListener listener);
    oneway void setOnConnectionStatusChangeListener(in IOnConnectionStatusChangeListener listener);
    oneway void setOnConnectToServerListener(in IOnConnectToServerListener listener);

    oneway void setOnUserInfoUpdateListener(in IOnUserInfoUpdateListener listener);
    oneway void setOnGroupInfoUpdateListener(in IOnGroupInfoUpdateListener listener);
    oneway void setOnGroupMembersUpdateListener(in IOnGroupMembersUpdateListener listener);
    oneway void setOnFriendUpdateListener(in IOnFriendUpdateListener listener);
    oneway void setOnSettingUpdateListener(in IOnSettingUpdateListener listener);
    oneway void setOnChannelInfoUpdateListener(in IOnChannelInfoUpdateListener listener);
    oneway void setOnConferenceEventListener(in IOnConferenceEventListener listener);

    oneway void setOnTrafficDataListener(in IOnTrafficDataListener listener);

    oneway void registerMessageContent(in String msgContentCls);
    oneway void registerMessageFlag(in int type, in int flag);

    oneway void send(in Message msg, in ISendMessageCallback callback, in int expireDuration);
    oneway void sendSavedMessage(in Message msg, in int expireDuration, in ISendMessageCallback callback);
    boolean cancelSendingMessage(in long messageId);
    oneway void recall(in long messageUid, IGeneralCallback callback);
    long getServerDeltaTime();
    boolean isConnectedToMainNetwork();
    List<ConversationInfo> getConversationList(in int[] conversationTypes, in int[] lines, in boolean lastMessage);
    oneway void getConversationListAsync(in int[] conversationTypes, in int[] lines, in IGetConversationListCallback callback);
    ConversationInfo getConversation(in int conversationType, in String target, in int line);
    long getFirstUnreadMessageId(in int conversationType, in String target, in int line);
    List<Message> getMessages(in Conversation conversation, in long fromIndex, in boolean before, in int count, in String withUser);
    List<Message> getMessagesEx(in int[] conversationTypes, in int[] lines, in int[] contentTypes, in long fromIndex, in boolean before, in int count, in String withUser);
    List<Message> getMessagesEx2(in int[] conversationTypes, in int[] lines, in int[] messageStatus, in long fromIndex, in boolean before, in int count, in String withUser);
    List<Message> getMessagesInStatusSync(in Conversation conversation, in int[] messageStatus, in long fromIndex, in boolean before, in int count, in String withUser);

    oneway void getMessagesAsync(in Conversation conversation, in long fromIndex, in boolean before, in int count, in String withUser, in IGetMessageCallback callback);
    oneway void getMentionedMessagesAsync(in Conversation conversation, in long fromIndex, in boolean before, in int count, in IGetMessageCallback callback);
    oneway void getMessagesInTypesAsync(in Conversation conversation, in int[] contentTypes, in long fromIndex, in boolean before, in int count, in String withUser, in IGetMessageCallback callback);
    oneway void getMessagesInStatusAsync(in Conversation conversation, in int[] messageStatus, in long fromIndex, in boolean before, in int count, in String withUser, in IGetMessageCallback callback);
    oneway void getMessagesExAsync(in int[] conversationTypes, in int[] lines, in int[] contentTypes, in long fromIndex, in boolean before, in int count, in String withUser, in IGetMessageCallback callback);
    oneway void getMessagesEx2Async(in int[] conversationTypes, in int[] lines, in int[] messageStatus, in long fromIndex, in boolean before, in int count, in String withUser, in IGetMessageCallback callback);
    oneway void getMessagesInTypesAndTimestampAsync(in Conversation conversation, in int[] contentTypes, in long timestamp, in boolean before, in int count, in String withUser, in IGetMessageCallback callback);

    oneway void getUserMessages(in String userId, in Conversation conversation, in long fromIndex, in boolean before, in int count, in IGetMessageCallback callback);
    oneway void getUserMessagesEx(in String userId, in int[] conversationTypes, in int[] lines, in int[] contentTypes, in long fromIndex, in boolean before, in int count, in IGetMessageCallback callback);

    oneway void getRemoteMessages(in Conversation conversation, in int[] contentTypes, in long beforeMessageUid, in int count, in IGetRemoteMessagesCallback callback);
    oneway void getRemoteMessage(in long messageUid, in IGetRemoteMessagesCallback callback);
    oneway void getConversationFileRecords(in Conversation conversation, in String fromUser, in long beforeMessageUid, in int order, in int count, in IGetFileRecordCallback callback);
    oneway void getMyFileRecords(in long beforeMessageUid, in int order, in int count, in IGetFileRecordCallback callback);
    oneway void deleteFileRecord(in long messageUid, in IGeneralCallback callback);
    oneway void searchFileRecords(in String keyword, in Conversation conversation, in String fromUser, in long beforeMessageUid, in int order, in int count, in IGetFileRecordCallback callback);
    oneway void searchMyFileRecords(in String keyword, in long beforeMessageUid, in int order, in int count, in IGetFileRecordCallback callback);
    oneway void clearRemoteConversationMessage(in Conversation conversation, in IGeneralCallback callback);

    Message getMessage(in long messageId);
    Message getMessageByUid(in long messageUid);

    Message insertMessage(in Message message, in boolean notify);
    boolean updateMessageContent(in Message message);
    boolean updateMessageContentAndTime(in Message message);
    boolean updateMessageStatus(in long messageId, in int messageStatus);

    UnreadCount getUnreadCount(in int conversationType, in String target, in int line);
    UnreadCount getUnreadCountEx(in int[] conversationTypes, in int[] lines);
    boolean clearUnreadStatus(in int conversationType, in String target, in int line);
    boolean clearUnreadStatusEx(in int[] conversationTypes, in int[] lines);
    boolean clearMessageUnreadStatus(long messageId);
    boolean clearUnreadStatusBeforeMessage(long messageId, in Conversation conversation);
    void clearAllUnreadStatus();
    boolean markAsUnRead(in int conversationType, in String target, in int line, in boolean sync);
    void clearMessages(in int conversationType, in String target, in int line);
    void clearMessagesEx(in int conversationType, in String target, in int line, in long before);
    void clearMessagesEx2(in int conversationType, in String target, in int line, in int keepCount);
    void clearAllMessages(in boolean removeConversation);
    void setMediaMessagePlayed(in long messageId);
    boolean setMessageLocalExtra(in long messageId, in String extra);
    void removeConversation(in int conversationType, in String target, in int line, in boolean clearMsg);
    oneway void setConversationTop(in int conversationType, in String target, in int line, in int top, in IGeneralCallback callback);
    void setConversationDraft(in int conversationType, in String target, in int line, in String draft);
    oneway void setConversationSilent(in int conversationType, in String target, in int line, in boolean silent,  in IGeneralCallback callback);
    void setConversationTimestamp(in int conversationType, in String target, in int line, in long timestamp);

    Map getConversationRead(in int conversationType, in String target, in int line);
    Map getMessageDelivery(in int conversationType, in String target);
    oneway void searchUser(in String keyword, in int searchType, in int page, in ISearchUserCallback callback);

    boolean isMyFriend(in String userId);
    List<String> getMyFriendList(in boolean refresh);
    List<Friend> getFriendList(in boolean refresh);
    oneway void loadFriendRequestFromRemote();

    String getUserSetting(in int scope, in String key);
    Map getUserSettings(in int scope);
    oneway void setUserSetting(in int scope, in String key, in String value, in IGeneralCallback callback);
    oneway void startLog();
    oneway void stopLog();
    oneway void setDeviceToken(in String token, in int pushType);

    List<FriendRequest> getFriendRequest(in boolean incomming);
    List<FriendRequest> getAllFriendRequest();
    FriendRequest getOneFriendRequest(in String userId, in boolean incomming);
    boolean clearFriendRequest(in boolean direction, in long beforeTime);
    boolean deleteFriendRequest(in String userId, in boolean direction);
    String getFriendAlias(in String userId);
    oneway void setFriendAlias(in String userId, in String alias, in IGeneralCallback callback);
    String getFriendExtra(in String userId);
    void clearUnreadFriendRequestStatus();
    int getUnreadFriendRequestStatus();
    oneway void removeFriend(in String userId, in IGeneralCallback callback);
    oneway void sendFriendRequest(in String userId, in String reason, in String extra, in IGeneralCallback callback);
    oneway void handleFriendRequest(in String userId, in boolean accept, in String extra, in IGeneralCallback callback);
    oneway void deleteFriend(in String userId, in IGeneralCallback callback);

    boolean isBlackListed(in String userId);
    List<String> getBlackList(in boolean refresh);
    oneway void setBlackList(in String userId, in boolean isBlacked, in IGeneralCallback callback);

    oneway void joinChatRoom(in String chatRoomId, in IGeneralCallback callback);
    oneway void quitChatRoom(in String chatRoomId, in IGeneralCallback callback);

    oneway void getChatRoomInfo(in String chatRoomId, in long updateDt, in IGetChatRoomInfoCallback callback);
    oneway void getChatRoomMembersInfo(in String chatRoomId, in int maxCount, in IGetChatRoomMembersInfoCallback callback);
    GroupInfo getGroupInfo(in String groupId, in boolean refresh);
    List<GroupInfo> getGroupInfos(in List<String> groupIds, in boolean refresh);
    oneway void getGroupInfoEx(in String groupId, in boolean refresh, in IGetGroupCallback callback);
    UserInfo getUserInfo(in String userId, in String groupId, in boolean refresh);
    List<UserInfo> getUserInfos(in List<String> userIds, in String groupId);
    oneway void getUserInfoEx(in String userId, in String groupId, in boolean refresh, in IGetUserCallback callback);

    oneway void uploadMedia(in String fileName, in byte[] data, int mediaType, in IUploadMediaCallback callback);
    oneway void uploadMediaFile(in String mediaPath, int mediaType, in IUploadMediaCallback callback);
    oneway void modifyMyInfo(in List<ModifyMyInfoEntry> values, in IGeneralCallback callback);
    boolean deleteMessage(in long messageId);
    boolean batchDeleteMessages(in long[] messageUids);
    boolean clearUserMessage(in String userId, in long start, in long end);
    void deleteRemoteMessage(in long messageUid, in IGeneralCallback callback);
    void updateRemoteMessageContent(in long messageUid, in MessagePayload payload, in boolean distribute, in boolean updateLocal, in IGeneralCallback callback);
    List<ConversationSearchResult> searchConversation(in String keyword, in int[] conversationTypes, in int[] lines);
    List<ConversationSearchResult> searchConversationEx(in String keyword, in int[] conversationTypes, in int[] lines, in long startTime, in long endTime, in boolean desc, in int limit, in int offset);
    List<ConversationSearchResult> searchConversationEx2(in String keyword, in int[] conversationTypes, in int[] lines, in int[] contentTypes, in long startTime, in long endTime, in boolean desc, in int limit, in int offset, in boolean onlyMentionedMsg);
    List<Message> searchMessage(in Conversation conversation, in String keyword, in boolean desc, in int limit, in int offset, in String withUser);
    List<Message> searchMentionedMessages(in Conversation conversation, in String keyword, in boolean desc, in int limit, in int offset);
    List<Message> searchMessageByTypes(in Conversation conversation, in String keyword, in int[] contentTypes, in boolean desc, in int limit, in int offset, in String withUser);
    List<Message> searchMessageByTypesAndTimes(in Conversation conversation, in String keyword, in int[] contentTypes, in long startTime, in long endTime, in boolean desc, in int limit, in int offset, in String withUser);
    oneway void searchMessagesEx(in int[] conversationTypes, in int[] lines, in int[] contentTypes, in String keyword, in long fromIndex, in boolean before, in int count, in String withUser, in IGetMessageCallback callback);
    oneway void searchMentionedMessagesEx(in int[] conversationTypes, in int[] lines, in String keyword, in boolean desc, in int limit, in int offset, in IGetMessageCallback callback);

    List<GroupSearchResult> searchGroups(in String keyword);
    List<UserInfo> searchFriends(in String keyworkd);

    String getEncodedClientId();

    oneway void createGroup(in String groupId, in String groupName, in String groupPortrait, in int groupType, in String groupExtra, in List<String> memberIds, in String memberExtra, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback2 callback);
    oneway void addGroupMembers(in String groupId, in List<String> memberIds, in String extra, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void removeGroupMembers(in String groupId, in List<String> memberIds, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void quitGroup(in String groupId, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void dismissGroup(in String groupId, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void modifyGroupInfo(in String groupId, in int modifyType, in String newValue, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void modifyGroupAlias(in String groupId, in String newAlias, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void modifyGroupMemberAlias(in String groupId, in String memberId, in String newAlias, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void modifyGroupMemberExtra(in String groupId, in String memberId, in String extra, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    List<GroupMember> getGroupMembers(in String groupId, in boolean forceUpdate);
    List<GroupMember> getGroupMembersByType(in String groupId, in int type);
    List<GroupMember> getGroupMembersByCount(in String groupId, in int count);
    GroupMember getGroupMember(in String groupId, in String memberId);
    oneway void getGroupMemberEx(in String groupId, in boolean forceUpdate, in IGetGroupMemberCallback callback);
    oneway void transferGroup(in String groupId, in String newOwner, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void setGroupManager(in String groupId, in boolean isSet, in List<String> memberIds, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void muteOrAllowGroupMember(in String groupId, in boolean isSet, in List<String> memberIds, in boolean isAllow, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    String getGroupRemark(in String groupId);
    oneway void setGroupRemark(in String groupId, in String remark, in IGeneralCallback callback);
    oneway void getMyGroups(in IGeneralCallback3 callback);
    oneway void getCommonGroups(in String userId, in IGeneralCallback3 callback);

    byte[] encodeData(in byte[] data);
    byte[] decodeData(in byte[] data);
    byte[] decodeDataEx(in int type, in byte[] data, in boolean gzip);

    String getHost();
    int getPort();
    String getHostEx();
    oneway void createChannel(in String channelId, in String channelName, in String channelPortrait, in String desc, in String extra, in ICreateChannelCallback callback);
    oneway void modifyChannelInfo(in String channelId, in int modifyType, in String newValue, in IGeneralCallback callback);
    ChannelInfo getChannelInfo(in String channelId, in boolean refresh);
    oneway void searchChannel(in String keyword, in ISearchChannelCallback callback);
    boolean isListenedChannel(in String channelId);
    oneway void listenChannel(in String channelId, in boolean listen, in IGeneralCallback callback);
    oneway void destoryChannel(in String channelId, in IGeneralCallback callback);
    List<String> getMyChannels();
    List<String> getListenedChannels();
    oneway void getRemoteListenedChannels(in IGeneralCallback3 callback);
    oneway void requireLock(in String lockId, in long duration, in IGeneralCallback callback);
    oneway void releaseLock(in String lockId, in IGeneralCallback callback);

    oneway void createSecretChat(in String userId, in ICreateSecretChatCallback callback);
    oneway void destroySecretChat(in String targetId, in IGeneralCallback callback);
    SecretChatInfo getSecretChatInfo(String targetId);

    String getImageThumbPara();

    void kickoffPCClient(in String pcClientId, in IGeneralCallback callback);

    oneway void getAuthCode(in String appId, in int appType, in String host, in IGeneralCallback2 callback);
    oneway void configApplication(in String appId, in int appType, in long timestamp, in String nonceStr, in String signature, in IGeneralCallback callback);

    oneway void getAuthorizedMediaUrl(in long messageUid, in int mediaType, in String mediaPath, in IGetAuthorizedMediaUrlCallback callback);
    oneway void getUploadUrl(in String fileName, in int mediaType, in String contentType, in IGetUploadUrlCallback callback);

    boolean isSupportBigFilesUpload();

    int getMessageCount(in Conversation conversation);
    boolean begainTransaction();
    boolean commitTransaction();
    boolean rollbackTransaction();

    boolean isCommercialServer();
    boolean isReceiptEnabled();
    boolean isGroupReceiptEnabled();
    boolean isGlobalDisableSyncDraft();
    boolean isEnableSecretChat();
    boolean isEnableUserOnlineState();
    void sendConferenceRequest(in long sessionId, in String roomId, in String request, in boolean advanced, in String data, in IGeneralCallback2 callback);
    void useSM4();
    void useAES256();
    void useTcpShortLink();
    void useRawMsg();
    void noUseFts();
    void checkSignature();

    String getProtoRevision();

    oneway void setProxyInfo(in Socks5ProxyInfo proxyInfo);

    oneway void watchUserOnlineState(in int conversationType, in String[] targets, in int duration, in IWatchUserOnlineStateCallback callback);
    oneway void unwatchOnlineState(in int conversationType, in String[] targets, in IGeneralCallback callback);
    oneway void setUserOnlineEventListener(in IOnUserOnlineEventListener listener);
    oneway void setSecretChatStateChangedListener(in IOnSecretChatStateListener listener);
    oneway void setSecretMessageBurnStateListener(in IOnSecretMessageBurnStateListener listener);
    oneway void setSecretChatBurnTime(in String targetId, int burnTime);
    BurnMessageInfo getBurnMessageInfo(in long messageId);
    byte[] decodeSecretChatData(in String targetid, in byte[] mediaData);

    oneway void decodeSecretChatDataAsync(in String targetId, in ParcelFileDescriptor pfd, in int length, in IGeneralCallbackInt callback);

    oneway void setDefaultPortraitProviderClass(in String clazz);
}
