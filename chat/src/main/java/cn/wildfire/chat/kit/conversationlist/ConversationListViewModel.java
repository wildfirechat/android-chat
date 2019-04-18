package cn.wildfire.chat.kit.conversationlist;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UnreadCount;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnConnectionStatusChangeListener;
import cn.wildfirechat.remote.OnConversationInfoUpdateListener;
import cn.wildfirechat.remote.OnGroupInfoUpdateListener;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.OnSendMessageListener;
import cn.wildfirechat.remote.OnSettingUpdateListener;
import cn.wildfirechat.remote.RemoveMessageListener;

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
        OnConversationInfoUpdateListener, OnConnectionStatusChangeListener {
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
            unreadCountMap = new HashMap<>();
        }

        loadUnreadCount();
        return unreadCountLiveData;
    }

    public MutableLiveData<Integer> connectionStatusLiveData() {
        return connectionStatusLiveData;
    }

    private Map<Conversation, UnreadCount> unreadCountMap;

    private void loadUnreadCount() {
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<ConversationInfo> conversations = ChatManager.Instance().getConversationList(types, lines);
            if (conversations != null) {
                for (ConversationInfo info : conversations) {
                    unreadCountMap.put(info.conversation, info.unreadCount);
                }
                postUnreadCount();
            }
        });
    }

    private void postUnreadCount() {
        if (unreadCountLiveData == null) {
            return;
        }
        UnreadCount unreadCount = new UnreadCount();
        if (unreadCountMap.isEmpty()) {
            return;
        }
        for (Map.Entry<Conversation, UnreadCount> entry : unreadCountMap.entrySet()) {
            unreadCount.unread += entry.getValue().unread;
            unreadCount.unreadMention += entry.getValue().unreadMention;
            unreadCount.unreadMentionAll += entry.getValue().unreadMentionAll;
        }

        unreadCountLiveData.postValue(unreadCount);
    }

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        if (messages != null && messages.size() > 0) {
            for (Message message : messages) {
                if (message.messageId == 0) {
                    continue;
                }
                Conversation conversation = message.conversation;
                if (types.contains(conversation.type) && lines.contains(conversation.line)) {
                    ConversationInfo conversationInfo = ChatManager.Instance().getConversation(message.conversation);
                    postConversationInfo(conversationInfo);
                }
            }
        }
    }

    @Override
    public void onRecallMessage(Message message) {
        Conversation conversation = message.conversation;
        if (types.contains(conversation.type) && lines.contains(conversation.line)) {
            ConversationInfo conversationInfo = ChatManager.Instance().getConversation(message.conversation);
            postConversationInfo(conversationInfo);
        }
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
        if (conversationRemovedLiveData != null) {
            conversationRemovedLiveData.setValue(conversationInfo.conversation);
        }
        ChatManager.Instance().removeConversation(conversationInfo.conversation, false);
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
        if (unreadCountMap != null && unreadCountMap.containsKey(conversationInfo.conversation)) {
            unreadCountMap.put(conversationInfo.conversation, new UnreadCount());
        }
        ChatManager.Instance().clearUnreadStatus(conversationInfo.conversation);
        postUnreadCount();
    }

    private void postConversationInfo(ConversationInfo conversationInfo) {
        if (conversationInfoLiveData != null) {
            UIUtils.postTaskSafely(() -> conversationInfoLiveData.setValue(conversationInfo));
        }

        if (unreadCountLiveData != null) {
            unreadCountMap.put(conversationInfo.conversation, conversationInfo.unreadCount);
            postUnreadCount();
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

        // 会话同步
        if (unreadCountLiveData != null) {
            loadUnreadCount();
        }
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
}
