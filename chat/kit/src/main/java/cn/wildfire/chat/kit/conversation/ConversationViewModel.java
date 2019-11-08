package cn.wildfire.chat.kit.conversation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.common.AppScopeViewModel;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetRemoteMessageCallback;

public class ConversationViewModel extends ViewModel implements AppScopeViewModel {
    private MutableLiveData<Conversation> clearConversationMessageLiveData;

    public MutableLiveData<Conversation> clearConversationMessageLiveData() {
        if (clearConversationMessageLiveData == null) {
            clearConversationMessageLiveData = new MutableLiveData<>();
        }
        return clearConversationMessageLiveData;
    }

    public ConversationViewModel() {
    }

    @Override
    protected void onCleared() {
    }

    public MutableLiveData<List<UiMessage>> loadOldMessages(Conversation conversation, String withUser, long fromMessageId, long fromMessageUid, int count) {
        MutableLiveData<List<UiMessage>> result = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<Message> messageList = ChatManager.Instance().getMessages(conversation, fromMessageId, true, count, withUser);
            if (messageList != null && !messageList.isEmpty()) {
                List<UiMessage> messages = new ArrayList<>();
                for (Message msg : messageList) {
                    messages.add(new UiMessage(msg));
                }
                result.postValue(messages);
            } else {
                ChatManager.Instance().getRemoteMessages(conversation, fromMessageUid, count, new GetRemoteMessageCallback() {
                    @Override
                    public void onSuccess(List<Message> messages) {
                        if (messages != null && !messages.isEmpty()) {
                            List<UiMessage> msgs = new ArrayList<>();
                            for (Message msg : messages) {
                                msgs.add(new UiMessage(msg));
                            }
                            result.postValue(msgs);
                        } else {
                            result.postValue(new ArrayList<UiMessage>());
                        }
                    }

                    @Override
                    public void onFail(int errorCode) {
                        result.postValue(new ArrayList<UiMessage>());
                    }
                });
            }
        });
        return result;
    }

    public LiveData<List<Message>> loadRemoteHistoryMessage(Conversation conversation, long fromMessageUid, int count) {
        MutableLiveData<List<Message>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            ChatManager.Instance().getRemoteMessages(conversation, fromMessageUid, count, new GetRemoteMessageCallback() {
                @Override
                public void onSuccess(List<Message> messages) {
                    data.setValue(messages);
                }

                @Override
                public void onFail(int errorCode) {
                    data.setValue(new ArrayList<>());
                }
            });
        });
        return data;
    }

    public MutableLiveData<List<UiMessage>> loadAroundMessages(Conversation conversation, String withUser, long focusIndex, int count) {
        MutableLiveData<List<UiMessage>> result = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<Message> oldMessageList = ChatManager.Instance().getMessages(conversation, focusIndex, true, count, withUser);
            List<UiMessage> oldMessages = new ArrayList<>();
            if (oldMessageList != null) {
                for (Message msg : oldMessageList) {
                    oldMessages.add(new UiMessage(msg));
                }
            }
            Message message = ChatManager.Instance().getMessage(focusIndex);
            List<Message> newMessageList = ChatManager.Instance().getMessages(conversation, focusIndex, false, count, withUser);
            List<UiMessage> newMessages = new ArrayList<>();
            if (newMessageList != null) {
                for (Message msg : newMessageList) {
                    newMessages.add(new UiMessage(msg));
                }
            }

            List<UiMessage> messages = new ArrayList<>();
            messages.addAll(oldMessages);
            if (message != null) {
                messages.add(new UiMessage(message));
            }
            messages.addAll(newMessages);
            result.postValue(messages);
        });

        return result;
    }

    public MutableLiveData<List<UiMessage>> loadNewMessages(Conversation conversation, String withUser, long startIndex, int count) {
        MutableLiveData<List<UiMessage>> result = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<Message> messageList = ChatManager.Instance().getMessages(conversation, startIndex, false, count, withUser);
            List<UiMessage> messages = new ArrayList<>();
            if (messageList != null) {
                for (Message msg : messageList) {
                    messages.add(new UiMessage(msg));
                }
            }
            result.postValue(messages);
        });
        return result;
    }

    public void clearUnreadStatus(Conversation conversation) {
        ChatManager.Instance().clearUnreadStatus(conversation);
    }

    public void clearConversationMessage(Conversation conversation) {
        ChatManager.Instance().clearMessages(conversation);
        if (clearConversationMessageLiveData != null) {
            clearConversationMessageLiveData.setValue(conversation);
        }
    }

    public ConversationInfo getConversationInfo(Conversation conversation) {
        return ChatManager.Instance().getConversation(conversation);
    }


    public MutableLiveData<List<UiMessage>> getMessages(Conversation conversation, String withUser) {
        return loadOldMessages(conversation, withUser, 0, 0, 20);
    }

    public void saveDraft(Conversation conversation, String draftString) {
        ChatManager.Instance().setConversationDraft(conversation, draftString);
    }

    public void setConversationSilent(Conversation conversation, boolean silent) {
        ChatManager.Instance().setConversationSilent(conversation, silent);
    }

}
