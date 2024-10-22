/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.viewmodel;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.audio.AudioPlayManager;
import cn.wildfire.chat.kit.audio.IAudioPlayListener;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.message.viewholder.AudioMessageContentViewHolder;
import cn.wildfire.chat.kit.third.location.data.LocationData;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.LocationMessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.StickerMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ReadEntry;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallbackBytes;
import cn.wildfirechat.remote.OnClearMessageListener;
import cn.wildfirechat.remote.OnDeleteMessageListener;
import cn.wildfirechat.remote.OnMessageDeliverListener;
import cn.wildfirechat.remote.OnMessageReadListener;
import cn.wildfirechat.remote.OnMessageUpdateListener;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.OnSendMessageListener;
import cn.wildfirechat.remote.SecretMessageBurnStateListener;
import cn.wildfirechat.remote.SendMessageCallback;

public class MessageViewModel extends ViewModel implements OnReceiveMessageListener,
    OnSendMessageListener,
    OnDeleteMessageListener,
    OnRecallMessageListener,
    OnMessageUpdateListener,
    OnMessageDeliverListener,
    OnMessageReadListener,
    OnClearMessageListener,
    SecretMessageBurnStateListener {
    private MutableLiveData<List<UiMessage>> messageLiveData;
    private MutableLiveData<UiMessage> messageUpdateLiveData;
    private MutableLiveData<UiMessage> messageRemovedLiveData;
    private MutableLiveData<Map<String, String>> mediaUploadedLiveData;
    private MutableLiveData<Object> clearMessageLiveData;
    private MutableLiveData<Map<String, Long>> messageDeliverLiveData;
    private MutableLiveData<List<ReadEntry>> messageReadLiveData;
    private MutableLiveData<Pair<String, Long>> messageStartBurnLiveData;
    private MutableLiveData<List<Long>> messageBurnedLiveData;

    private Message playingAudioMessage;

    public MessageViewModel() {
        ChatManager.Instance().addOnReceiveMessageListener(this);
        ChatManager.Instance().addRecallMessageListener(this);
        ChatManager.Instance().addSendMessageListener(this);
        ChatManager.Instance().addOnMessageUpdateListener(this);
        ChatManager.Instance().addClearMessageListener(this);
        ChatManager.Instance().addMessageDeliverListener(this);
        ChatManager.Instance().addMessageReadListener(this);
        ChatManager.Instance().addSecretMessageBurnStateListener(this);
        ChatManager.Instance().addDeleteMessageListener(this);
    }

    @Override
    protected void onCleared() {
        ChatManager.Instance().removeOnReceiveMessageListener(this);
        ChatManager.Instance().removeRecallMessageListener(this);
        ChatManager.Instance().removeSendMessageListener(this);
        ChatManager.Instance().removeOnMessageUpdateListener(this);
        ChatManager.Instance().removeClearMessageListener(this);
        ChatManager.Instance().removeMessageDeliverListener(this);
        ChatManager.Instance().removeMessageReadListener(this);
        ChatManager.Instance().removeSecretMessageBurnStateListener(this);
        ChatManager.Instance().removeDeleteMessageListener(this);
    }

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        if (messageLiveData != null && messages != null) {
            postNewMessage(toUIMessages(messages));
        }
    }

    public MutableLiveData<List<UiMessage>> messageLiveData() {
        if (messageLiveData == null) {
            messageLiveData = new MutableLiveData<>();
        }
        return messageLiveData;
    }

    public MutableLiveData<UiMessage> messageUpdateLiveData() {
        if (messageUpdateLiveData == null) {
            messageUpdateLiveData = new MutableLiveData<>();
        }
        return messageUpdateLiveData;
    }

    public MutableLiveData<UiMessage> messageRemovedLiveData() {
        if (messageRemovedLiveData == null) {
            messageRemovedLiveData = new MutableLiveData<>();
        }
        return messageRemovedLiveData;
    }

    public MutableLiveData<Map<String, String>> mediaUpdateLiveData() {
        if (mediaUploadedLiveData == null) {
            mediaUploadedLiveData = new MutableLiveData<>();
        }
        return mediaUploadedLiveData;
    }

    public MutableLiveData<Object> clearMessageLiveData() {
        if (clearMessageLiveData == null) {
            clearMessageLiveData = new MutableLiveData<>();
        }
        return clearMessageLiveData;
    }

    public MutableLiveData<Map<String, Long>> messageDeliverLiveData() {
        if (messageDeliverLiveData == null) {
            messageDeliverLiveData = new MutableLiveData<>();
        }
        return messageDeliverLiveData;
    }

    public MutableLiveData<List<ReadEntry>> messageReadLiveData() {
        if (messageReadLiveData == null) {
            messageReadLiveData = new MutableLiveData<>();
        }
        return messageReadLiveData;
    }

    public MutableLiveData<Pair<String, Long>> messageStartBurnLiveData() {
        if (messageStartBurnLiveData == null) {
            messageStartBurnLiveData = new MutableLiveData<>();
        }
        return messageStartBurnLiveData;
    }

    public MutableLiveData<List<Long>> messageBurnedLiveData() {
        if (messageBurnedLiveData == null) {
            messageBurnedLiveData = new MutableLiveData<>();
        }
        return messageBurnedLiveData;
    }

    @Override
    public void onRecallMessage(Message message) {
        if (message != null) {
            UiMessage uiMessage = new UiMessage(message);
            if (playingAudioMessage != null && playingAudioMessage.messageUid == message.messageUid) {
                stopPlayAudio();
            }
            postMessageUpdate(uiMessage);
        }
    }

    // TODO 可优化成moveTo
    public void resendMessage(Message message) {
        deleteMessage(message);
        sendMessage(message.conversation, message.content);
    }

    public void recallMessage(Message message) {
        ChatManager.Instance().recallMessage(message, new GeneralCallback() {
            @Override
            public void onSuccess() {
                Message msg = message;
                if (message.messageId > 0) {
                    msg = ChatManager.Instance().getMessage(message.messageId);
                }
                postMessageUpdate(new UiMessage(msg));
            }

            @Override
            public void onFail(int errorCode) {
                Log.e(MessageViewModel.class.getSimpleName(), "撤回失败: " + errorCode);
                // TODO 撤回失败
            }
        });
    }

    @Override
    public void onDeleteMessage(Message message) {
        if (messageRemovedLiveData != null) {
            messageRemovedLiveData.setValue(new UiMessage(message));
        }
    }

    /**
     * 删除消息
     *
     * @param message
     */
    public void deleteMessage(Message message) {
        if (messageRemovedLiveData != null) {
            messageRemovedLiveData.setValue(new UiMessage(message));
        }
        ChatManager.Instance().deleteMessage(message);
    }

    public void deleteRemoteMessage(Message message) {
        ChatManager.Instance().deleteRemoteMessage(message.messageUid, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (messageRemovedLiveData != null) {
                    messageRemovedLiveData.setValue(new UiMessage(message));
                }
            }

            @Override
            public void onFail(int errorCode) {
                Log.e("Message", "delete remote message error: " + errorCode);
            }
        });
    }

    public void playAudioMessage(UiMessage message) {
        if (message == null || !(message.message.content instanceof SoundMessageContent)) {
            return;
        }

        if (playingAudioMessage != null && playingAudioMessage.equals(message.message)) {
            AudioPlayManager.getInstance().stopPlay();
            message.continuousPlayAudio = false;
            playingAudioMessage = null;
            return;
        }

        playingAudioMessage = message.message;
        if (message.message.direction == MessageDirection.Receive && message.message.status != MessageStatus.Played) {
            message.message.status = MessageStatus.Played;
            ChatManager.Instance().setMediaMessagePlayed(message.message.messageId);
            message.continuousPlayAudio = true;
        }

        File file = DownloadManager.mediaMessageContentFile(message.message);

        if (file == null) {
            return;
        }
        if (file.exists()) {
            playAudio(message, file);
        } else {
            Log.e("ConversationViewHolder", "audio not exist");
        }
    }

    public void stopPlayAudio() {
        AudioPlayManager.getInstance().stopPlay();
    }

    public void sendMessage(Conversation conversation, List<String> toUsers, MessageContent content) {
        Message msg = new Message();
        msg.conversation = conversation;
        msg.content = content;
        if (toUsers != null) {
            msg.toUsers = toUsers.toArray(new String[0]);
        }
        sendMessage(msg);
    }

    public void sendMessage(Conversation conversation, MessageContent content) {
        this.sendMessage(conversation, null, content);
    }

    public MutableLiveData<OperateResult<Void>> sendMessageEx(Message message) {
        MutableLiveData<OperateResult<Void>> result = new MutableLiveData<>();
        ChatManager.Instance().sendMessage(message, 0, new SendMessageCallback() {
            @Override
            public void onSuccess(long messageUid, long timestamp) {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(-1));
            }

            @Override
            public void onPrepare(long messageId, long savedTime) {

            }
        });
        return result;
    }

    public void sendMessage(Message message) {
        // the call back would be called on the ui thread
        message.sender = ChatManager.Instance().getUserId();
        ChatManager.Instance().sendMessage(message, null);
    }

    public void sendTextMsg(Conversation conversation, TextMessageContent txtContent) {
        this.sendTextMsg(conversation, null, txtContent);
    }

    public void sendTextMsg(Conversation conversation, List<String> toUsers, TextMessageContent txtContent) {
        Message message = new Message();
        message.conversation = conversation;
        message.content = txtContent;
        if (toUsers != null) {
            message.toUsers = toUsers.toArray(new String[0]);
        }
        sendMessage(message);
        ChatManager.Instance().setConversationDraft(conversation, null);
    }

    public void saveDraft(Conversation conversation, String draftString) {
        ChatManager.Instance().setConversationDraft(conversation, draftString);
    }

    public void setConversationSilent(Conversation conversation, boolean silent) {
        ChatManager.Instance().setConversationSilent(conversation, silent);
    }

    public void sendImgMsg(Conversation conversation, Uri imageFileThumbUri, Uri imageFileSourceUri) {
        this.sendImgMsg(conversation, null, imageFileThumbUri, imageFileSourceUri);
    }

    public void sendImgMsg(Conversation conversation, List<String> toUsers, Uri imageFileThumbUri, Uri imageFileSourceUri) {
        ImageMessageContent imgContent = new ImageMessageContent(imageFileSourceUri.getEncodedPath());
        String thumbParam = ChatManager.Instance().getImageThumbPara();
        if (!TextUtils.isEmpty(thumbParam)) {
            imgContent.setThumbPara(ChatManager.Instance().getImageThumbPara());
        }
        sendMessage(conversation, toUsers, imgContent);
    }

    public void sendImgMsg(Conversation conversation, File imageFileThumb, File imageFileSource) {
        this.sendImgMsg(conversation, null, imageFileSource, imageFileSource);
    }

    public void sendImgMsg(Conversation conversation, List<String> toUsers, File imageFileThumb, File imageFileSource) {
        // Uri.fromFile()遇到中文檔名會轉 ASCII，這個 ASCII 的 path 將導致後面 ChatManager.sendMessage()
        // 在 new File()時找不到 File 而 return
        Uri imageFileThumbUri = Uri.parse(Uri.decode(imageFileThumb.getAbsolutePath()));
//        Uri imageFileThumbUri = Uri.fromFile(imageFileThumb);
        Uri imageFileSourceUri = Uri.parse(Uri.decode(imageFileSource.getAbsolutePath()));
//        Uri imageFileSourceUri = Uri.fromFile(imageFileSource);
        sendImgMsg(conversation, toUsers, imageFileThumbUri, imageFileSourceUri);

    }

    public void sendVideoMsg(Conversation conversation, File file) {
        this.sendVideoMsg(conversation, null, file);
    }

    public void sendVideoMsg(Conversation conversation, List<String> toUsers, File file) {
        VideoMessageContent videoMessageContent = new VideoMessageContent(file.getPath());
        sendMessage(conversation, videoMessageContent);
    }

    public void sendStickerMsg(Conversation conversation, String localPath, String remoteUrl) {
        this.sendStickerMsg(conversation, null, localPath, remoteUrl);
    }

    public void sendStickerMsg(Conversation conversation, List<String> toUsers, String localPath, String remoteUrl) {
        StickerMessageContent stickerMessageContent = new StickerMessageContent(localPath);
        stickerMessageContent.remoteUrl = remoteUrl;
        sendMessage(conversation, toUsers, stickerMessageContent);
    }

    public void sendFileMsg(Conversation conversation, File file) {
        this.sendFileMsg(conversation, null, file);
    }

    public void sendFileMsg(Conversation conversation, List<String> toUsers, File file) {
        FileMessageContent fileMessageContent = new FileMessageContent(file.getPath());
        sendMessage(conversation, toUsers, fileMessageContent);
    }

    public void sendLocationMessage(Conversation conversation, LocationData locationData) {
        this.sendLocationMessage(conversation, null, locationData);
    }

    public void sendLocationMessage(Conversation conversation, List<String> toUsers, LocationData locationData) {
        LocationMessageContent locCont = new LocationMessageContent();
        locCont.setTitle(locationData.getPoi());
        locCont.getLocation().setLatitude(locationData.getLat());
        locCont.getLocation().setLongitude(locationData.getLng());
        locCont.setThumbnail(locationData.getThumbnail());

        sendMessage(conversation, toUsers, locCont);
    }

    public void sendAudioFile(Conversation conversation, Uri audioPath, int duration) {
        this.sendAudioFile(conversation, null, audioPath, duration);
    }

    public void sendAudioFile(Conversation conversation, List<String> toUsers, Uri audioPath, int duration) {
        if (audioPath != null) {
            File file = new File(audioPath.getPath());
            if (!file.exists() || file.length() == 0L) {
                Log.e("ConversationViewModel", "send audio file fail");
                return;
            }
            SoundMessageContent soundContent = new SoundMessageContent(file.getAbsolutePath());
            soundContent.setDuration(duration);
            sendMessage(conversation, toUsers, soundContent);
        }
    }

    private void playAudio(UiMessage message, File file) {
        Uri uri = Uri.fromFile(file);
        IAudioPlayListener audioPlayListener = new IAudioPlayListener() {
            @Override
            public void onStart(Uri var1) {
                if (uri.equals(var1)) {
                    message.isPlaying = true;
                    postMessageUpdate(message);
                }
            }

            @Override
            public void onStop(Uri var1) {
                if (uri.equals(var1)) {
                    message.isPlaying = false;
                    playingAudioMessage = null;
                    postMessageUpdate(message);
                }
            }

            @Override
            public void onComplete(Uri var1) {
                if (uri.equals(var1)) {
                    message.isPlaying = false;
                    message.audioPlayCompleted = true;
                    playingAudioMessage = null;
                    postMessageUpdate(message);
                }
            }
        };

        if (message.message.conversation.type == Conversation.ConversationType.SecretChat) {
            byte[] encryptedBytes = FileUtils.readBytesFromFile(file.getAbsolutePath());
            ChatManager.Instance().decodeSecretDataAsync(message.message.conversation.target, encryptedBytes, new GeneralCallbackBytes() {
                @Override
                public void onSuccess(byte[] data) {
                    AudioPlayManager.getInstance().startPlay(WfcUIKit.getWfcUIKit().getApplication(), uri, data, audioPlayListener);
                }

                @Override
                public void onFail(int errorCode) {
                    message.isDownloading = false;
                    Log.d("MessageVideModel", "decodeSecretDataAsync error " + errorCode);
                }
            });

        } else {
            AudioPlayManager.getInstance().startPlay(WfcUIKit.getWfcUIKit().getApplication(), uri, audioPlayListener);
        }
    }

    public void downloadMedia(UiMessage message, File targetFile) {
        MessageContent content = message.message.content;
        if (!(content instanceof MediaMessageContent)) {
            return;
        }

        if (message.isDownloading) {
            return;
        }
        message.isDownloading = true;
        postMessageUpdate(message);

        DownloadManager.download(((MediaMessageContent) content).remoteUrl, targetFile.getParent(), targetFile.getName(), new DownloadManager.OnDownloadListener() {
            @Override
            public void onSuccess(File file) {
                message.isDownloading = false;
                message.progress = 100;
                postMessageUpdate(message);
            }

            @Override
            public void onProgress(int percent) {
                message.progress = percent;
                postMessageUpdate(message);
            }

            @Override
            public void onFail() {
                message.isDownloading = false;
                message.progress = 0;
                postMessageUpdate(message);
                Log.e(AudioMessageContentViewHolder.class.getSimpleName(), "download failed: " + message.message.messageId);
            }
        });
    }

    private void postMessageUpdate(UiMessage message) {
        if (message == null || message.message == null) {
            return;
        }
        if (messageUpdateLiveData != null) {
            UIUtils.postTaskSafely(() -> messageUpdateLiveData.setValue(message));
        }
    }

    private void postNewMessage(List<UiMessage> messages) {
        messageLiveData.setValue(messages);
    }

    @Override
    public void onSendSuccess(Message message) {
        postMessageUpdate(new UiMessage(message));
    }

    @Override
    public void onSendFail(Message message, int errorCode) {
        postMessageUpdate(new UiMessage(message));
    }

    @Override
    public void onSendPrepare(Message message, long savedTime) {
        postNewMessage(Collections.singletonList(new UiMessage(message)));
    }

    @Override
    public void onProgress(Message message, long uploaded, long total) {
        UiMessage uiMessage = new UiMessage(message);
        uiMessage.progress = (int) (uploaded * 100 / total);
        postMessageUpdate(uiMessage);
    }

    @Override
    public void onMediaUpload(Message message, String remoteUrl) {
        String key;
        MediaMessageContent content = (MediaMessageContent) message.content;
        if (message.conversation.type == Conversation.ConversationType.SecretChat) {
            key = message.conversation.target + "_" + content.localPath;
        } else {
            key = content.localPath;
        }
        SharedPreferences sharedPreferences = ChatManager.Instance().getApplicationContext().getSharedPreferences("sticker", Context.MODE_PRIVATE);
        sharedPreferences.edit()
            .putString(key, content.remoteUrl)
            .apply();

        if (mediaUploadedLiveData != null) {
            Map<String, String> map = new HashMap<>();
            map.put(((MediaMessageContent) message.content).localPath, remoteUrl);
            UIUtils.postTaskSafely(() -> mediaUploadedLiveData.setValue(map));
        }
    }

    @Override
    public void onMessageUpdate(Message message) {
        postMessageUpdate(new UiMessage(message));
    }

    @Override
    public void onClearMessage(Conversation conversation) {
        if (clearMessageLiveData != null) {
            clearMessageLiveData.postValue(new Object());
        }
    }

    @Override
    public void onMessageDelivered(Map<String, Long> deliveries) {
        if (messageDeliverLiveData != null) {
            messageDeliverLiveData.postValue(deliveries);
        }
    }

    @Override
    public void onMessageRead(List<ReadEntry> readEntries) {
        if (messageReadLiveData != null) {
            messageReadLiveData.postValue(readEntries);
        }
    }

    @Override
    public void onSecretMessageStartBurning(String targetId, long playedMsgId) {
        if (messageStartBurnLiveData != null) {
            messageStartBurnLiveData.postValue(new Pair<>(targetId, playedMsgId));
        }
    }

    @Override
    public void onSecretMessageBurned(List<Long> messageIds) {
        if (messageBurnedLiveData != null) {
            messageBurnedLiveData.postValue(messageIds);
        }
    }

    private List<UiMessage> toUIMessages(List<Message> messages) {
        return messages.stream().map(UiMessage::new).collect(Collectors.toList());
    }
}
