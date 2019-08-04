// IRemoteClient.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.client.ISendMessageCallback;
import cn.wildfirechat.client.ISearchUserCallback;
import cn.wildfirechat.client.IGeneralCallback;
import cn.wildfirechat.client.IGeneralCallback2;
import cn.wildfirechat.client.IGeneralCallback3;
import cn.wildfirechat.client.IUploadMediaCallback;
import cn.wildfirechat.client.IOnReceiveMessageListener;
import cn.wildfirechat.client.IOnConnectionStatusChangeListener;
import cn.wildfirechat.client.IGetChatRoomInfoCallback;
import cn.wildfirechat.client.IGetChatRoomMembersInfoCallback;
import cn.wildfirechat.client.IGetGroupInfoCallback;
import cn.wildfirechat.client.ICreateChannelCallback;
import cn.wildfirechat.client.ISearchChannelCallback;
import cn.wildfirechat.client.IGetRemoteMessageCallback;

import cn.wildfirechat.client.IOnFriendUpdateListener;
import cn.wildfirechat.client.IOnGroupInfoUpdateListener;
import cn.wildfirechat.client.IOnGroupMembersUpdateListener;
import cn.wildfirechat.client.IOnSettingUpdateListener;
import cn.wildfirechat.client.IGetGroupsCallback;
import cn.wildfirechat.client.IOnUserInfoUpdateListener;
import cn.wildfirechat.client.IOnChannelInfoUpdateListener;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.model.UnreadCount;

import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationSearchResult;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.ChannelInfo;


import java.util.List;
import java.util.Map;



// Declare any non-default types here with import statements

interface IRemoteClient {
    String getClientId();
    boolean connect(in String userId, in String token);
    oneway void disconnect(in boolean clearSession);
    oneway void setForeground(in int isForeground);
    oneway void onNetworkChange();
    oneway void setServerAddress(in String host, in int port);

    oneway void setOnReceiveMessageListener(in IOnReceiveMessageListener listener);
    oneway void setOnConnectionStatusChangeListener(in IOnConnectionStatusChangeListener listener);

    oneway void setOnUserInfoUpdateListener(in IOnUserInfoUpdateListener listener);
    oneway void setOnGroupInfoUpdateListener(in IOnGroupInfoUpdateListener listener);
    oneway void setOnGroupMembersUpdateListener(in IOnGroupMembersUpdateListener listener);
    oneway void setOnFriendUpdateListener(in IOnFriendUpdateListener listener);
    oneway void setOnSettingUpdateListener(in IOnSettingUpdateListener listener);
    oneway void setOnChannelInfoUpdateListener(in IOnChannelInfoUpdateListener listener);

    oneway void registerMessageContent(in String msgContentCls);

    oneway void send(in Message msg, in ISendMessageCallback callback, in int expireDuration);
    oneway void recall(in long messageUid, IGeneralCallback callback);
    long getServerDeltaTime();
    List<ConversationInfo> getConversationList(in int[] conversationTypes, in int[] lines);
    ConversationInfo getConversation(in int conversationType, in String target, in int line);
    List<Message> getMessages(in Conversation conversation, in long fromIndex, in boolean before, in int count, in String withUser);
    List<Message> getMessagesEx(in int[] conversationTypes, in int[] lines, in int[] contentTypes, in long fromIndex, in boolean before, in int count, in String withUser);
    List<Message> getMessagesEx2(in int[] conversationTypes, in int[] lines, in int messageStatus, in long fromIndex, in boolean before, in int count, in String withUser);

    oneway void getRemoteMessages(in Conversation conversation, in long beforeMessageUid, in int count, in IGetRemoteMessageCallback callback);

    Message getMessage(in long messageId);
    Message getMessageByUid(in long messageUid);

    Message insertMessage(in Message message, in boolean notify);
    boolean updateMessage(in Message message);

    UnreadCount getUnreadCount(in int conversationType, in String target, in int line);
    UnreadCount getUnreadCountEx(in int[] conversationTypes, in int[] lines);
    oneway void clearUnreadStatus(in int conversationType, in String target, in int line);
    oneway void clearAllUnreadStatus();
    oneway void clearMessages(in int conversationType, in String target, in int line);
    oneway void setMediaMessagePlayed(in long messageId);
    oneway void removeConversation(in int conversationType, in String target, in int line, in boolean clearMsg);
    oneway void setConversationTop(in int conversationType, in String target, in int line, in boolean top);
    oneway void setConversationDraft(in int conversationType, in String target, in int line, in String draft);
    oneway void setConversationSilent(in int conversationType, in String target, in int line, in boolean silent);

    oneway void searchUser(in String keyword, in boolean fuzzy, in ISearchUserCallback callback);

    boolean isMyFriend(in String userId);
    List<String> getMyFriendList(in boolean refresh);
    List<UserInfo> getMyFriendListInfo(in boolean refresh);
    oneway void loadFriendRequestFromRemote();

    String getUserSetting(in int scope, in String key);
    Map getUserSettings(in int scope);
    oneway void setUserSetting(in int scope, in String key, in String value, in IGeneralCallback callback);
    oneway void startLog();
    oneway void stopLog();
    oneway void setDeviceToken(in String token, in int pushType);

    List<FriendRequest> getFriendRequest(in boolean incomming);
    String getFriendAlias(in String userId);
    oneway void setFriendAlias(in String userId, in String alias, in IGeneralCallback callback);
    oneway void clearUnreadFriendRequestStatus();
    int getUnreadFriendRequestStatus();
    oneway void removeFriend(in String userId, in IGeneralCallback callback);
    oneway void sendFriendRequest(in String userId, in String reason, in IGeneralCallback callback);
    oneway void handleFriendRequest(in String userId, in boolean accept, in IGeneralCallback callback);
    oneway void deleteFriend(in String userId, in IGeneralCallback callback);

    boolean isBlackListed(in String userId);
    List<String> getBlackList(in boolean refresh);
    oneway void setBlackList(in String userId, in boolean isBlacked, in IGeneralCallback callback);

    oneway void joinChatRoom(in String chatRoomId, in IGeneralCallback callback);
    oneway void quitChatRoom(in String chatRoomId, in IGeneralCallback callback);

    oneway void getChatRoomInfo(in String chatRoomId, in long updateDt, in IGetChatRoomInfoCallback callback);
    oneway void getChatRoomMembersInfo(in String chatRoomId, in int maxCount, in IGetChatRoomMembersInfoCallback callback);
    GroupInfo getGroupInfo(in String groupId, in boolean refresh);
    UserInfo getUserInfo(in String userId, in String groupId, in boolean refresh);
    List<UserInfo> getUserInfos(in List<String> userIds, in String groupId);

    oneway void uploadMedia(in byte[] data, int mediaType, in IUploadMediaCallback callback);
    oneway void modifyMyInfo(in List<ModifyMyInfoEntry> values, in IGeneralCallback callback);
    boolean deleteMessage(in long messageId);
    List<ConversationSearchResult> searchConversation(in String keyword, in int[] conversationTypes, in int[] lines);
    List<Message> searchMessage(in Conversation conversation, in String keyword);

    List<GroupSearchResult> searchGroups(in String keyword);
    List<UserInfo> searchFriends(in String keyworkd);

    String getEncodedClientId();

    oneway void createGroup(in String groupId, in String groupName, in String groupPortrait, in int groupType, in List<String> memberIds, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback2 callback);
    oneway void addGroupMembers(in String groupId, in List<String> memberIds, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void removeGroupMembers(in String groupId, in List<String> memberIds, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void quitGroup(in String groupId, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void dismissGroup(in String groupId, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void modifyGroupInfo(in String groupId, in int modifyType, in String newValue, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void modifyGroupAlias(in String groupId, in String newAlias, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    List<GroupMember> getGroupMembers(in String groupId, in boolean forceUpdate);
    GroupMember getGroupMember(in String groupId, in String memberId);
    oneway void transferGroup(in String groupId, in String newOwner, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    oneway void setGroupManager(in String groupId, in boolean isSet, in List<String> memberIds, in int[] notifyLines, in MessagePayload notifyMsg, in IGeneralCallback callback);
    byte[] encodeData(in byte[] data);
    byte[] decodeData(in byte[] data);
    oneway void createChannel(in String channelId, in String channelName, in String channelPortrait, in String desc, in String extra, in ICreateChannelCallback callback);
    oneway void modifyChannelInfo(in String channelId, in int modifyType, in String newValue, in IGeneralCallback callback);
    ChannelInfo getChannelInfo(in String channelId, in boolean refresh);
    oneway void searchChannel(in String keyword, in ISearchChannelCallback callback);
    boolean isListenedChannel(in String channelId);
    oneway void listenChannel(in String channelId, in boolean listen, in IGeneralCallback callback);
    oneway void destoryChannel(in String channelId, in IGeneralCallback callback);
    List<String> getMyChannels();
    List<String> getListenedChannels();
}
