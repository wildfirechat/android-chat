package cn.wildfire.chat.kit.conversationlist;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.notification.DismissGroupNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.QuitGroupNotificationContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UnreadCount;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnClearMessageListener;
import cn.wildfirechat.remote.OnConnectionStatusChangeListener;
import cn.wildfirechat.remote.OnConversationInfoUpdateListener;
import cn.wildfirechat.remote.OnGroupInfoUpdateListener;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.OnRemoveConversationListener;
import cn.wildfirechat.remote.OnSendMessageListener;
import cn.wildfirechat.remote.OnSettingUpdateListener;
import cn.wildfirechat.remote.RemoveMessageListener;
import cn.wildfirechat.remote.UserSettingScope;

/**
 * how
 * 1. observe conversationInfoLiveData in your activity or fragment, but if you still not called getConversationList,
 * just ignore the data.
 * 2. call getConversationList
 */
public class ConversationListViewModel extends ViewModel implements OnReceiveMessageListener,
        OnSendMessageListener,
        OnRecallMessageListener,
        RemoveMessageListener,
        OnSettingUpdateListener,
        OnGroupInfoUpdateListener,
        OnConversationInfoUpdateListener,
        OnRemoveConversationListener,
        OnConnectionStatusChangeListener,
        OnClearMessageListener {
    private MutableLiveData<ConversationInfo> conversationInfoLiveData;
    private MutableLiveData<Conversation> conversationRemovedLiveData;
    private MutableLiveData<UnreadCount> unreadCountLiveData;
    private MutableLiveData<Object> settingUpdateLiveData;
    private MutableLiveData<Integer> connectionStatusLiveData = new MutableLiveData<>();

    private List<Conversation.ConversationType> types;
    private List<Integer> lines;

    public ConversationListViewModel(List<Conversation.ConversationType> types, List<Integer> lines) {
        super();
        this.types = types;
        this.lines = lines;
        ChatManager.Instance().addOnReceiveMessageListener(this);
        ChatManager.Instance().addSendMessageListener(this);
        ChatManager.Instance().addConversationInfoUpdateListener(this);
        ChatManager.Instance().addSettingUpdateListener(this);
        ChatManager.Instance().addRecallMessageListener(this);
        ChatManager.Instance().addConnectionChangeListener(this);
        ChatManager.Instance().addRemoveMessageListener(this);
        ChatManager.Instance().addGroupInfoUpdateListener(this);
        ChatManager.Instance().addClearMessageListener(this);
        ChatManager.Instance().addRemoveConversationListener(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeOnReceiveMessageListener(this);
        ChatManager.Instance().removeSendMessageListener(this);
        ChatManager.Instance().removeConversationInfoUpdateListener(this);
        ChatManager.Instance().removeSettingUpdateListener(this);
        ChatManager.Instance().removeConnectionChangeListener(this);
        ChatManager.Instance().removeRecallMessageListener(this);
        ChatManager.Instance().removeRemoveMessageListener(this);
        ChatManager.Instance().removeGroupInfoUpdateListener(this);
        ChatManager.Instance().removeClearMessageListener(this);
        ChatManager.Instance().removeRemoveConversationListener(this);
    }

    public LiveData<List<ConversationInfo>> getConversationListAsync(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        MutableLiveData<List<ConversationInfo>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<ConversationInfo> conversationInfos = ChatManager.Instance().getConversationList(conversationTypes, lines);
            data.postValue(conversationInfos);
        });
        return data;
    }

    public List<ConversationInfo> getConversationList(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        return ChatManager.Instance().getConversationList(conversationTypes, lines);
    }

    public MutableLiveData<ConversationInfo> conversationInfoLiveData() {
        if (conversationInfoLiveData == null) {
            conversationInfoLiveData = new MutableLiveData<>();
        }
        return conversationInfoLiveData;
    }

    public MutableLiveData<Conversation> conversationRemovedLiveData() {
        if (conversationRemovedLiveData == null) {
            conversationRemovedLiveData = new MutableLiveData<>();
        }
        return conversationRemovedLiveData;
    }

    public MutableLiveData<Object> settingUpdateLiveData() {
        if (settingUpdateLiveData == null) {
            settingUpdateLiveData = new MutableLiveData<>();
        }
        return settingUpdateLiveData;
    }

    public MutableLiveData<UnreadCount> unreadCountLiveData() {
        if (unreadCountLiveData == null) {
            unreadCountLiveData = new MutableLiveData<>();
        }

        loadUnreadCount();
        return unreadCountLiveData;
    }

    public MutableLiveData<Integer> connectionStatusLiveData() {
        return connectionStatusLiveData;
    }


    private void loadUnreadCount() {
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<ConversationInfo> conversations = ChatManager.Instance().getConversationList(types, lines);
            if (conversations != null) {
                UnreadCount unreadCount = new UnreadCount();
                for (ConversationInfo info : conversations) {
                    unreadCount.unread += info.unreadCount.unread;
                    unreadCount.unreadMention += info.unreadCount.unreadMention;
                    unreadCount.unreadMentionAll += info.unreadCount.unreadMentionAll;
                }
                postUnreadCount(unreadCount);
            }
        });
    }

    private void postUnreadCount(UnreadCount unreadCount) {
        if (unreadCountLiveData == null) {
            return;
        }
        unreadCountLiveData.postValue(unreadCount);
    }

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        Map<String, String> settings = ChatManager.Instance().getUserSettings(UserSettingScope.Conversation_Sync);
        if (settings == null || settings.isEmpty()) {
            return;
        }

        if (messages != null && messages.size() > 0) {
            ChatManager.Instance().getWorkHandler().post(() -> {
                Map<Conversation, Long> toUpdateConversationMap = new HashMap<>();
                String userId = ChatManager.Instance().getUserId();
                for (Message message : messages) {
                    Conversation conversation = message.conversation;
                    if (!types.contains(conversation.type) && !lines.contains(conversation.line)) {
                        continue;
                    }

                    if (message.messageId == 0) {
                        continue;
                    }

                    if ((message.content instanceof QuitGroupNotificationContent && ((QuitGroupNotificationContent) message.content).operator.equals(userId))
                            || (message.content instanceof KickoffGroupMemberNotificationContent && ((KickoffGroupMemberNotificationContent) message.content).kickedMembers.contains(userId))
                            || message.content instanceof DismissGroupNotificationContent) {
                        continue;
                    }

                    Long uid = toUpdateConversationMap.get(message.conversation);
                    if (uid == null || message.messageUid > uid) {
                        toUpdateConversationMap.put(message.conversation, message.messageUid);
                    }
                }

                for (Conversation conversation : toUpdateConversationMap.keySet()) {
                    ConversationInfo conversationInfo = ChatManager.Instance().getConversation(conversation);
                    postConversationInfo(conversationInfo);

                }
            });
            loadUnreadCount();
        }
    }

    @Override
    public void onRecallMessage(Message message) {
        Conversation conversation = message.conversation;
        if (types.contains(conversation.type) && lines.contains(conversation.line)) {
            ConversationInfo conversationInfo = ChatManager.Instance().getConversation(message.conversation);
            postConversationInfo(conversationInfo);
        }
        loadUnreadCount();
    }


    @Override
    public void onSendSuccess(Message message) {
        Conversation conversation = message.conversation;
        if (types.contains(conversation.type) && lines.contains(conversation.line)) {
            if (message.messageId > 0) {
                ConversationInfo conversationInfo = ChatManager.Instance().getConversation(message.conversation);
                postConversationInfo(conversationInfo);
            }
        }
    }

    @Override
    public void onSendFail(Message message, int errorCode) {
        Conversation conversation = message.conversation;
        if (types.contains(conversation.type) && lines.contains(conversation.line)) {
            if (message.messageId > 0) {
                ConversationInfo conversationInfo = ChatManager.Instance().getConversation(message.conversation);
                postConversationInfo(conversationInfo);
            }
        }
    }

    @Override
    public void onSendPrepare(Message message, long savedTime) {
        Conversation conversation = message.conversation;
        if (types.contains(conversation.type) && lines.contains(conversation.line)) {
            if (message.messageId > 0) {
                ConversationInfo conversationInfo = ChatManager.Instance().getConversation(message.conversation);
                postConversationInfo(conversationInfo);
            }
        }
    }

    public void removeConversation(ConversationInfo conversationInfo) {
        Conversation conversation = conversationInfo.conversation;
        if (!types.contains(conversation.type) || !lines.contains(conversation.line)) {
            Log.e(ConversationListViewModel.class.getSimpleName(), "this conversationListViewModel can not remove the target conversation");
            return;
        }
        ChatManager.Instance().clearUnreadStatus(conversation);
        ChatManager.Instance().removeConversation(conversationInfo.conversation, false);
        loadUnreadCount();
    }

    public void clearMessages(Conversation conversation) {
        ChatManager.Instance().clearMessages(conversation);
        loadUnreadCount();
    }

    public void unSubscribeChannel(ConversationInfo conversationInfo) {
        ChatManager.Instance().listenChannel(conversationInfo.conversation.target, false, new GeneralCallback() {
            @Override
            public void onSuccess() {
                removeConversation(conversationInfo);
            }

            @Override
            public void onFail(int errorCode) {
                // do nothing
            }
        });
    }

    public void setConversationTop(ConversationInfo conversationInfo, boolean top) {
        ChatManager.Instance().setConversationTop(conversationInfo.conversation, top);
    }

    /**
     * @param conversationInfo
     */
    public void clearConversationUnreadStatus(ConversationInfo conversationInfo) {
        UnreadCount unreadCount = conversationInfo.unreadCount;
        if (unreadCount.unread == 0 && unreadCount.unreadMentionAll == 0 && unreadCount.unreadMention == 0) {
            return;
        }
        ChatManager.Instance().clearUnreadStatus(conversationInfo.conversation);
        loadUnreadCount();
    }

    private void postConversationInfo(ConversationInfo conversationInfo) {
        if (conversationInfo == null) {
            return;
        }
        if (conversationInfoLiveData != null) {
            conversationInfoLiveData.postValue(conversationInfo);
        }
    }

    @Override
    public void onMessagedRemove(Message message) {
        ConversationInfo conversationInfo = ChatManager.Instance().getConversation(message.conversation);
        postConversationInfo(conversationInfo);
    }

    @Override
    public void onConversationDraftUpdate(ConversationInfo conversationInfo, String draft) {
        postConversationInfo(conversationInfo);
    }

    @Override
    public void onConversationTopUpdate(ConversationInfo conversationInfo, boolean top) {
        postConversationInfo(conversationInfo);
    }

    @Override
    public void onConversationSilentUpdate(ConversationInfo conversationInfo, boolean silent) {
        postConversationInfo(conversationInfo);
    }

    @Override
    public void onConversationUnreadStatusClear(ConversationInfo conversationInfo, UnreadCount unreadCount) {
        postConversationInfo(conversationInfo);
    }

    @Override
    public void onSettingUpdate() {
        if (settingUpdateLiveData != null) {
            UIUtils.postTaskSafely(() -> settingUpdateLiveData.setValue(new Object()));
        }

        // 可能是会话同步
        loadUnreadCount();
    }

    @Override
    public void onConnectionStatusChange(int status) {
        connectionStatusLiveData.postValue(status);
    }

    @Override
    public void onGroupInfoUpdate(List<GroupInfo> groupInfos) {
        if (!types.contains(Conversation.ConversationType.Group)) {
            return;
        }
        if (groupInfos != null) {
            for (GroupInfo groupInfo : groupInfos) {
                Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupInfo.target);
                ConversationInfo conversationInfo = ChatManager.Instance().getConversation(conversation);
                postConversationInfo(conversationInfo);
            }
        }
    }

    @Override
    public void onClearMessage(Conversation conversation) {
        ConversationInfo conversationInfo = ChatManager.Instance().getConversation(conversation);
        postConversationInfo(conversationInfo);
        loadUnreadCount();
    }

    @Override
    public void onConversationRemove(Conversation conversation) {
        if (conversationRemovedLiveData != null) {
            conversationRemovedLiveData.setValue(conversation);
        }
        loadUnreadCount();
    }
}
