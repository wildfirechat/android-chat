package cn.wildfire.chat.kit.conversationlist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.UnreadCount;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnClearMessageListener;
import cn.wildfirechat.remote.OnConnectionStatusChangeListener;
import cn.wildfirechat.remote.OnConversationInfoUpdateListener;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.OnRemoveConversationListener;
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
        OnConversationInfoUpdateListener,
        OnRemoveConversationListener,
        OnConnectionStatusChangeListener,
        OnClearMessageListener {
    private MutableLiveData<List<ConversationInfo>> conversationListLiveData;
    private MutableLiveData<UnreadCount> unreadCountLiveData;
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
        ChatManager.Instance().removeClearMessageListener(this);
        ChatManager.Instance().removeRemoveConversationListener(this);
    }

    private AtomicInteger loadingCount = new AtomicInteger(0);

    public void reloadConversationList() {
        reloadConversationList(false);
    }

    public void reloadConversationList(boolean force) {
        if (conversationListLiveData == null) {
            return;
        }
        if (!force) {
            int count = loadingCount.get();
            if (count > 0) {
                return;
            }
        }
        loadingCount.incrementAndGet();

        ChatManager.Instance().getWorkHandler().post(() -> {
            loadingCount.decrementAndGet();
            List<ConversationInfo> conversationInfos = ChatManager.Instance().getConversationList(types, lines);
            conversationListLiveData.postValue(conversationInfos);
        });
    }

    public List<ConversationInfo> getConversationList(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        return ChatManager.Instance().getConversationList(conversationTypes, lines);
    }

    public MutableLiveData<List<ConversationInfo>> conversationListLiveData() {
        if (conversationListLiveData == null) {
            conversationListLiveData = new MutableLiveData<>();
        }
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<ConversationInfo> conversationInfos = ChatManager.Instance().getConversationList(types, lines);
            conversationListLiveData.postValue(conversationInfos);
        });

        return conversationListLiveData;
    }

    public MutableLiveData<UnreadCount> unreadCountLiveData() {
        if (unreadCountLiveData == null) {
            unreadCountLiveData = new MutableLiveData<>();
        }

        reloadConversationUnreadStatus();
        return unreadCountLiveData;
    }

    public MutableLiveData<Integer> connectionStatusLiveData() {
        return connectionStatusLiveData;
    }

    public void reloadConversationUnreadStatus() {
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<ConversationInfo> conversations = ChatManager.Instance().getConversationList(types, lines);
            if (conversations != null) {
                UnreadCount unreadCount = new UnreadCount();
                for (ConversationInfo info : conversations) {
                    if (!info.isSilent) {
                        unreadCount.unread += info.unreadCount.unread;
                    }
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
        reloadConversationList(true);
        reloadConversationUnreadStatus();
    }

    @Override
    public void onRecallMessage(Message message) {
        reloadConversationList();
        reloadConversationUnreadStatus();
    }


    @Override
    public void onSendSuccess(Message message) {
        reloadConversationList();
    }

    @Override
    public void onSendFail(Message message, int errorCode) {
        reloadConversationList();
    }

    @Override
    public void onSendPrepare(Message message, long savedTime) {
        Conversation conversation = message.conversation;
        if (types.contains(conversation.type) && lines.contains(conversation.line)) {
            if (message.messageId > 0) {
                reloadConversationList();
            }
        }
    }

    public void removeConversation(ConversationInfo conversationInfo, boolean clearMsg) {
        ChatManager.Instance().clearUnreadStatus(conversationInfo.conversation);
        ChatManager.Instance().removeConversation(conversationInfo.conversation, clearMsg);
    }

    public void clearMessages(Conversation conversation) {
        ChatManager.Instance().clearMessages(conversation);
    }

    // TODO move the following to another class
    public void unSubscribeChannel(ConversationInfo conversationInfo) {
        ChatManager.Instance().listenChannel(conversationInfo.conversation.target, false, new GeneralCallback() {
            @Override
            public void onSuccess() {
                removeConversation(conversationInfo, false);
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

    @Override
    public void onMessagedRemove(Message message) {
        reloadConversationList();
        reloadConversationUnreadStatus();
    }

    @Override
    public void onConversationDraftUpdate(ConversationInfo conversationInfo, String draft) {
        reloadConversationList();
    }

    @Override
    public void onConversationTopUpdate(ConversationInfo conversationInfo, boolean top) {
        reloadConversationList();
    }

    @Override
    public void onConversationSilentUpdate(ConversationInfo conversationInfo, boolean silent) {
        reloadConversationList();
    }

    @Override
    public void onConversationUnreadStatusClear(ConversationInfo conversationInfo) {
        reloadConversationList();
        reloadConversationUnreadStatus();
    }

    @Override
    public void onSettingUpdate() {
        // 可能是会话同步
        reloadConversationList(true);
        reloadConversationUnreadStatus();
    }

    @Override
    public void onConnectionStatusChange(int status) {
        connectionStatusLiveData.postValue(status);
    }

    @Override
    public void onClearMessage(Conversation conversation) {
        reloadConversationList();
        reloadConversationUnreadStatus();
    }

    @Override
    public void onConversationRemove(Conversation conversation) {
        reloadConversationList();
        reloadConversationUnreadStatus();
    }
}
