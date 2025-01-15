/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import cn.wildfire.chat.kit.common.AppScopeViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.notification.TipNotificationContent;
import cn.wildfirechat.model.BurnMessageInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.CreateSecretChatCallback;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GetMessageCallback;
import cn.wildfirechat.remote.GetOneRemoteMessageCallback;
import cn.wildfirechat.remote.GetRemoteMessageCallback;
import cn.wildfirechat.remote.SecretChatStateChangeListener;

public class ConversationViewModel extends ViewModel implements AppScopeViewModel, SecretChatStateChangeListener {
    private MutableLiveData<Conversation> clearConversationMessageLiveData;
    private MutableLiveData<Pair<String, ChatManager.SecretChatState>> secretConversationStateLiveData;

    public ConversationViewModel() {
        ChatManager.Instance().addSecretChatStateChangedListener(this);
    }

    public MutableLiveData<Conversation> clearConversationMessageLiveData() {
        if (clearConversationMessageLiveData == null) {
            clearConversationMessageLiveData = new MutableLiveData<>();
        }
        return clearConversationMessageLiveData;
    }

    public MutableLiveData<Pair<String, ChatManager.SecretChatState>> secretConversationStateLiveData() {
        if (secretConversationStateLiveData == null) {
            secretConversationStateLiveData = new MutableLiveData<>();
        }
        return secretConversationStateLiveData;
    }

    @Override
    protected void onCleared() {
        ChatManager.Instance().removeSecretChatStateChangedListener(this);
    }

    // 包含不存储类型消息
    public MutableLiveData<List<UiMessage>> loadOldMessages(Conversation conversation, String withUser, long fromMessageId, long fromMessageUid, int count, boolean enableLoadRemoteMessageWhenNoMoreLocalOldMessage) {
        MutableLiveData<List<UiMessage>> result = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            ChatManager.Instance().getMessages(conversation, fromMessageId, true, count, withUser, new GetMessageCallback() {
                @Override
                public void onSuccess(List<Message> messageList, boolean hasMore) {
                    if (messageList != null && !messageList.isEmpty()) {
                        List<UiMessage> uiMsgs = new ArrayList<>();
                        for (Message msg : messageList) {
                            if (conversation.type == Conversation.ConversationType.SecretChat) {
                                BurnMessageInfo burnMessageInfo = ChatManager.Instance().getBurnMessageInfo(msg.messageId);
                                uiMsgs.add(new UiMessage(msg, burnMessageInfo));
                            } else {
                                uiMsgs.add(new UiMessage(msg));
                            }
                        }
                        result.setValue(uiMsgs);
                    } else if (enableLoadRemoteMessageWhenNoMoreLocalOldMessage && conversation.type != Conversation.ConversationType.SecretChat) {
                        ChatManager.Instance().getRemoteMessages(conversation, null, fromMessageUid, count, new GetRemoteMessageCallback() {
                            @Override
                            public void onSuccess(List<Message> messages) {
                                if (messages != null && !messages.isEmpty()) {
                                    List<UiMessage> uiMsgs = new ArrayList<>();
                                    for (Message msg : messages) {
//                                        if (msg.messageId != 0) {
                                        uiMsgs.add(new UiMessage(msg));
//                                        }
                                    }
                                    result.postValue(uiMsgs);
                                } else {
                                    result.postValue(new ArrayList<>());
                                }
                            }

                            @Override
                            public void onFail(int errorCode) {
                                result.postValue(new ArrayList<>());
                            }
                        });
                    } else {
                        result.postValue(new ArrayList<>());
                    }
                }

                @Override
                public void onFail(int errorCode) {
                    result.postValue(new ArrayList<>());
                }
            });
        });
        return result;
    }

    public LiveData<List<Message>> loadRemoteHistoryMessage(Conversation conversation, long fromMessageUid, int count) {
        MutableLiveData<List<Message>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            ChatManager.Instance().getRemoteMessages(conversation, null, fromMessageUid, count, new GetRemoteMessageCallback() {
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
            List<Message> oldMessageList = new ArrayList<>();
            List<Message> newMessageList = new ArrayList<>();
            CountDownLatch countDownLatch = new CountDownLatch(3);
            ChatManager.Instance().getMessages(conversation, focusIndex, true, count, withUser, new GetMessageCallback() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) {
                    oldMessageList.addAll(messages);
                    countDownLatch.countDown();
                }

                @Override
                public void onFail(int errorCode) {
                    countDownLatch.countDown();
                }
            });

            ChatManager.Instance().getMessages(conversation, focusIndex, false, count, withUser, new GetMessageCallback() {

                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) {
                    newMessageList.addAll(messages);
                    countDownLatch.countDown();
                }

                @Override
                public void onFail(int errorCode) {
                    countDownLatch.countDown();
                }
            });

            final Message[] focusMessage = {ChatManager.Instance().getMessage(focusIndex)};
            if (focusMessage[0] == null || focusMessage[0].content.notLoaded == 0) {
                countDownLatch.countDown();
            } else {
                ChatManager.Instance().getRemoteMessage(focusMessage[0].messageUid, new GetOneRemoteMessageCallback() {
                    @Override
                    public void onSuccess(Message message) {
                        focusMessage[0] = message;
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onFail(int errorCode) {
                        countDownLatch.countDown();
                    }
                });
            }

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<UiMessage> oldMessages = new ArrayList<>();
            for (Message msg : oldMessageList) {
                if (conversation.type == Conversation.ConversationType.SecretChat) {
                    BurnMessageInfo burnMessageInfo = ChatManager.Instance().getBurnMessageInfo(msg.messageId);
                    oldMessages.add(new UiMessage(msg, burnMessageInfo));
                } else {
                    oldMessages.add(new UiMessage(msg));
                }
            }

            List<UiMessage> newMessages = new ArrayList<>();
            for (Message msg : newMessageList) {
                if (conversation.type == Conversation.ConversationType.SecretChat) {
                    BurnMessageInfo burnMessageInfo = ChatManager.Instance().getBurnMessageInfo(msg.messageId);
                    newMessages.add(new UiMessage(msg, burnMessageInfo));
                } else {
                    newMessages.add(new UiMessage(msg));
                }
            }

            List<UiMessage> messages = new ArrayList<>();
            messages.addAll(oldMessages);
            TipNotificationContent tipNotificationContent = new TipNotificationContent();
            tipNotificationContent.tip = "--------- 以下是新消息 ----------";
            tipNotificationContent.fromSelf = true;
            Message tipMessage = new Message();
            tipMessage.conversation = conversation;
            tipMessage.content = tipNotificationContent;
            tipMessage.direction = MessageDirection.Send;
            tipMessage.messageId = Long.MAX_VALUE;
            messages.add(new UiMessage(tipMessage));

            if (focusMessage[0] != null) {
                messages.add(new UiMessage(focusMessage[0]));
            }
            messages.addAll(newMessages);
            result.postValue(messages);
        });

        return result;
    }

    public MutableLiveData<List<UiMessage>> loadNewMessages(Conversation conversation, String withUser, long startIndex, int count) {
        MutableLiveData<List<UiMessage>> result = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<UiMessage> uiMessages = new ArrayList<>();
            ChatManager.Instance().getMessages(conversation, startIndex, false, count, withUser, new GetMessageCallback() {
                @Override
                public void onSuccess(List<Message> messageList, boolean hasMore) {
                    List<UiMessage> uiMsgs = new ArrayList<>();
                    if (messageList != null) {
                        for (Message msg : messageList) {
                            if (conversation.type == Conversation.ConversationType.SecretChat) {
                                BurnMessageInfo burnMessageInfo = ChatManager.Instance().getBurnMessageInfo(msg.messageId);
                                uiMsgs.add(new UiMessage(msg, burnMessageInfo));
                            } else {
                                uiMsgs.add(new UiMessage(msg));
                            }
                        }
                        uiMessages.addAll(0, uiMsgs);
                        if (!hasMore) {
                            result.setValue(uiMessages);
                        }
                    }
                }

                @Override
                public void onFail(int errorCode) {

                }
            });
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

    public void clearRemoteConversationMessage(Conversation conversation) {
        ChatManager.Instance().clearRemoteConversationMessage(conversation, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (clearConversationMessageLiveData != null) {
                    clearConversationMessageLiveData.setValue(conversation);
                }
            }

            @Override
            public void onFail(int errorCode) {
                Log.e("Conversation", "clearRemoteConversation error: " + errorCode);
            }
        });
    }

    public ConversationInfo getConversationInfo(Conversation conversation) {
        return ChatManager.Instance().getConversation(conversation);
    }


    public MutableLiveData<List<UiMessage>> getMessages(Conversation conversation, String withUser, boolean enableLoadRemoteMessageWhenNoMoreLocalOldMessage) {
        return loadOldMessages(conversation, withUser, 0, 0, 20, enableLoadRemoteMessageWhenNoMoreLocalOldMessage);
    }

    public void saveDraft(Conversation conversation, String draftString) {
        ChatManager.Instance().setConversationDraft(conversation, draftString);
    }

    public void setConversationSilent(Conversation conversation, boolean silent) {
        ChatManager.Instance().setConversationSilent(conversation, silent);
    }

    public MutableLiveData<OperateResult<Pair<String, Integer>>> createSecretChat(String userId) {
        MutableLiveData<OperateResult<Pair<String, Integer>>> resultLiveData = new MutableLiveData<>();
        ChatManager.Instance().createSecretChat(userId, new CreateSecretChatCallback() {
            @Override
            public void onSuccess(String target, int line) {
                resultLiveData.postValue(new OperateResult<Pair<String, Integer>>(new Pair<>(target, line), 0));
            }

            @Override
            public void onFail(int errorCode) {
                resultLiveData.postValue(new OperateResult<Pair<String, Integer>>(null, errorCode));
            }
        });
        return resultLiveData;
    }

    @Override
    public void onSecretChatStateChanged(String targetId, ChatManager.SecretChatState state) {
        Pair<String, ChatManager.SecretChatState> pair = new Pair<>(targetId, state);
        if (secretConversationStateLiveData != null) {
            secretConversationStateLiveData.postValue(pair);
        }
    }
}
