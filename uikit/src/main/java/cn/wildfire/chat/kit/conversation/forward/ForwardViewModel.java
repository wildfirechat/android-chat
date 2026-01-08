/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.forward;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.message.ArticlesMessageContent;
import cn.wildfirechat.message.CallStartMessageContent;
import cn.wildfirechat.message.LinkMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class ForwardViewModel extends ViewModel {
    private static final String TAG = "ForwardViewModel";

    public MutableLiveData<OperateResult<Integer>> forward(Conversation targetConversation, Message... messages) {
        MutableLiveData<OperateResult<Integer>> resultMutableLiveData = new MutableLiveData<>();

        for (Message message : messages) {
            if (message == null) {
                continue;
            }
            message.conversation = targetConversation;
            if (message.content instanceof ArticlesMessageContent) {
                List<LinkMessageContent> contents = ((ArticlesMessageContent) message.content).toLinkMessageContent();
                for (LinkMessageContent content : contents) {
                    ChatManager.Instance().sendMessage(targetConversation, content, null, 0, null);
                }
            } else {
                message.content = filterMessageContent(message);
                ChatManager.Instance().sendMessage(message, null);
            }
        }
        resultMutableLiveData.postValue(new OperateResult<Integer>(0));
        return resultMutableLiveData;
    }

    public MutableLiveData<OperateResult<Integer>> forwardToMultipleTargets(
        List<Conversation> targetConversations,
        Message[] messages
    ) {
        MutableLiveData<OperateResult<Integer>> result = new MutableLiveData<>();

        new Thread(() -> {
            int successCount = 0;
            int failCount = 0;

            for (Conversation target : targetConversations) {
                try {
                    for (Message message : messages) {
                        if (message == null) {
                            continue;
                        }

                        Message msgCopy = cloneMessage(message);
                        msgCopy.conversation = target;

                        if (msgCopy.content instanceof ArticlesMessageContent) {
                            List<LinkMessageContent> contents = ((ArticlesMessageContent) msgCopy.content).toLinkMessageContent();
                            for (LinkMessageContent content : contents) {
                                ChatManager.Instance().sendMessage(target, content, null, 0, null);
                            }
                        } else {
                            msgCopy.content = filterMessageContent(msgCopy);
                            ChatManager.Instance().sendMessage(msgCopy, null);
                        }
                    }
                    successCount++;
                } catch (Exception e) {
                    Log.e(TAG, "Forward to " + target.target + " failed", e);
                    failCount++;
                }
            }

            if (failCount == 0) {
                result.postValue(new OperateResult<Integer>(0));
            } else {
                OperateResult<Integer> errorResult = new OperateResult<>(failCount);
                result.postValue(errorResult);
            }
        }).start();

        return result;
    }

    private Message cloneMessage(Message original) {
        Message copy = new Message();
        copy.content = original.content;
        copy.messageUid = original.messageUid;
        copy.direction = original.direction;
        copy.status = original.status;
        return copy;
    }

    public MessageContent filterMessageContent(Message msg) {
        MessageContent content = msg.content;
        if (content instanceof CallStartMessageContent) {
            content = new TextMessageContent(content.digest(msg));
        } else if (content instanceof SoundMessageContent) {
            content = new TextMessageContent(content.digest(msg) + " " + ((SoundMessageContent) content).getDuration() + "''");
        }
        return content;
    }
}
