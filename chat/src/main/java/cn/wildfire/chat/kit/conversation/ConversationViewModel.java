package cn.wildfire.chat.kit.conversation;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.app.MyApp;
import cn.wildfire.chat.app.third.location.data.LocationData;
import cn.wildfire.chat.kit.audio.AudioPlayManager;
import cn.wildfire.chat.kit.audio.IAudioPlayListener;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.message.viewholder.AudioMessageContentViewHolder;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadManager;
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
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GetRemoteMessageCallback;
import cn.wildfirechat.remote.OnClearMessageListener;
import cn.wildfirechat.remote.OnMessageUpdateListener;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.OnSendMessageListener;

public class ConversationViewModel extends ViewModel implements OnReceiveMessageListener,
        OnSendMessageListener,
        OnRecallMessageListener, OnMessageUpdateListener, OnClearMessageListener {
    private MutableLiveData<UiMessage> messageLiveData;
    private MutableLiveData<UiMessage> messageUpdateLiveData;
    private MutableLiveData<UiMessage> messageRemovedLiveData;
    private MutableLiveData<Map<String, String>> mediaUploadedLiveData;
    private MutableLiveData<Object> clearMessageLiveData;
    private Conversation conversation;
    //仅限于在Channel内使用。Channel的owner对订阅Channel单个用户发起一对一私聊
    private String channelPrivateChatUser;

    private Message toPlayAudioMessage;

    public ConversationViewModel(Conversation conversation, String channelPrivateChatUser) {
        this.conversation = conversation;
        this.channelPrivateChatUser = channelPrivateChatUser;
        ChatManager.Instance().addOnReceiveMessageListener(this);
        ChatManager.Instance().addRecallMessageListener(this);
        ChatManager.Instance().addSendMessageListener(this);
        ChatManager.Instance().addOnMessageUpdateListener(this);
        ChatManager.Instance().addClearMessageListener(this);
    }

    public void setConversation(Conversation conversation, String withUser) {
        this.conversation = conversation;
        this.channelPrivateChatUser = withUser;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public String getChannelPrivateChatUser() {
        return channelPrivateChatUser;
    }

    @Override
    protected void onCleared() {
        ChatManager.Instance().removeOnReceiveMessageListener(this);
        ChatManager.Instance().removeRecallMessageListener(this);
        ChatManager.Instance().removeSendMessageListener(this);
        ChatManager.Instance().removeOnMessageUpdateListener(this);
        ChatManager.Instance().removeClearMessageListener(this);
    }

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        if (messageLiveData != null && messages != null) {
            for (Message msg : messages) {
                if (isMessageInCurrentConversation(msg)) {
                    postNewMessage(new UiMessage(msg));
                }
            }
        }
    }

    // 需要在getMessages之前调用，不然可能会导致界面上少显示消息
    public MutableLiveData<UiMessage> messageLiveData() {
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

    public MutableLiveData<List<UiMessage>> loadOldMessages(long fromMessageId, long fromMessageUid, int count) {
        MutableLiveData<List<UiMessage>> result = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<Message> messageList = ChatManager.Instance().getMessages(conversation, fromMessageId, true, count, channelPrivateChatUser);
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

    public LiveData<List<Message>> loadRemoteHistoryMessage(long fromMessageUid, int count) {
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

    public MutableLiveData<List<UiMessage>> loadAroundMessages(long focusIndex, int count) {
        MutableLiveData<List<UiMessage>> result = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<Message> oldMessageList = ChatManager.Instance().getMessages(conversation, focusIndex, true, count, channelPrivateChatUser);
            List<UiMessage> oldMessages = new ArrayList<>();
            if (oldMessageList != null) {
                for (Message msg : oldMessageList) {
                    oldMessages.add(new UiMessage(msg));
                }
            }
            Message message = ChatManager.Instance().getMessage(focusIndex);
            List<Message> newMessageList = ChatManager.Instance().getMessages(conversation, focusIndex, false, count, channelPrivateChatUser);
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

    public MutableLiveData<List<UiMessage>> loadNewMessages(long startIndex, int count) {
        MutableLiveData<List<UiMessage>> result = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<Message> messageList = ChatManager.Instance().getMessages(conversation, startIndex, false, count, channelPrivateChatUser);
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

    public MutableLiveData<List<UiMessage>> getMessages() {
        return loadOldMessages(0, 0, 20);
    }

    // TODO 参数里面直接带上conversation相关信息，会更方便
    @Override
    public void onRecallMessage(Message message) {
        if (message != null && isMessageInCurrentConversation(message)) {
            UiMessage uiMessage = new UiMessage(message);
            postMessageUpdate(uiMessage);
        }
    }

    // TODO 可优化成moveTo
    public void resendMessage(Message message) {
        deleteMessage(message);
        sendMessage(message.content);
    }

    public void recallMessage(Message message) {
        ChatManager.Instance().recallMessage(message, new GeneralCallback() {
            @Override
            public void onSuccess() {
                Message msg = ChatManager.Instance().getMessage(message.messageId);
                postMessageUpdate(new UiMessage(msg));
            }

            @Override
            public void onFail(int errorCode) {
                Log.e(ConversationViewModel.class.getSimpleName(), "撤回失败: " + errorCode);
                // TODO 撤回失败
            }
        });
    }

    public void deleteMessage(Message message) {
        if (messageRemovedLiveData != null) {
            messageRemovedLiveData.setValue(new UiMessage(message));
        }
        ChatManager.Instance().deleteMessage(message);
    }

    public void clearUnreadStatus(Conversation conversation) {
        ChatManager.Instance().clearUnreadStatus(conversation);
    }

    public void clearConversationMessage(Conversation conversation) {
        ChatManager.Instance().clearMessages(conversation);
    }

    public ConversationInfo getConversationInfo(Conversation conversation) {
        return ChatManager.Instance().getConversation(conversation);
    }

    public void playAudioMessage(UiMessage message) {
        if (message == null || !(message.message.content instanceof SoundMessageContent)) {
            return;
        }

        if (toPlayAudioMessage != null && toPlayAudioMessage.equals(message.message)) {
            AudioPlayManager.getInstance().stopPlay();
            toPlayAudioMessage = null;
            return;
        }

        toPlayAudioMessage = message.message;
        if (message.message.direction == MessageDirection.Receive && message.message.status != MessageStatus.Played) {
            message.message.status = MessageStatus.Played;
            ChatManager.Instance().setMediaMessagePlayed(message.message.messageId);
        }

        File file = mediaMessageContentFile(message);

        if (file == null) {
            return;
        }
        if (file.exists()) {
            playAudio(message, file);
        } else {
            Log.e("ConversationViewHolder", "audio not exist");
        }
    }


    public void sendMessage(MessageContent content) {
        Message msg = new Message();
        msg.conversation = conversation;
        msg.content = content;
        if (!TextUtils.isEmpty(channelPrivateChatUser)) {
            msg.toUsers = new String[]{channelPrivateChatUser};
        }
        sendMessage(msg);
    }

    public void sendMessage(Message message) {
        // the call back would be called on the ui thread
        message.sender = ChatManager.Instance().getUserId();
        ChatManager.Instance().sendMessage(message, null);
    }

    public void sendTextMsg(TextMessageContent txtContent) {
        sendMessage(txtContent);
        ChatManager.Instance().setConversationDraft(conversation, null);
    }

    public void saveDraft(Conversation conversation, String draftString) {
        ChatManager.Instance().setConversationDraft(conversation, draftString);
    }

    public void setConversationSilent(Conversation conversation, boolean silent) {
        ChatManager.Instance().setConversationSilent(conversation, silent);
    }

    public void sendImgMsg(Uri imageFileThumbUri, Uri imageFileSourceUri) {
        ImageMessageContent imgContent = new ImageMessageContent(imageFileSourceUri.getEncodedPath());
        imgContent.setThumbnail(BitmapFactory.decodeFile(imageFileThumbUri.getEncodedPath()));
        sendMessage(imgContent);
    }

    public void sendImgMsg(File imageFileThumb, File imageFileSource) {
        Uri imageFileThumbUri = Uri.fromFile(imageFileThumb);
        Uri imageFileSourceUri = Uri.fromFile(imageFileSource);
        sendImgMsg(imageFileThumbUri, imageFileSourceUri);

    }

    public void sendVideoMsg(File file) {
        VideoMessageContent videoMessageContent = new VideoMessageContent(file.getPath());
        sendMessage(videoMessageContent);
    }

    public void sendStickerMsg(String localPath, String remoteUrl) {
        StickerMessageContent stickerMessageContent = new StickerMessageContent(localPath);
        stickerMessageContent.remoteUrl = remoteUrl;
        sendMessage(stickerMessageContent);
    }

    public void sendFileMsg(File file) {
        FileMessageContent fileMessageContent = new FileMessageContent(file.getPath());
        sendMessage(fileMessageContent);
    }

    public void sendLocationMessage(LocationData locationData) {
        LocationMessageContent locCont = new LocationMessageContent();
        locCont.setTitle(locationData.getPoi());
        locCont.getLocation().setLatitude(locationData.getLat());
        locCont.getLocation().setLongitude(locationData.getLng());
        locCont.setThumbnail(locationData.getThumbnail());

        sendMessage(locCont);
    }

    public void sendAudioFile(Uri audioPath, int duration) {
        if (audioPath != null) {
            File file = new File(audioPath.getPath());
            if (!file.exists() || file.length() == 0L) {
                Log.e("ConversationViewModel", "send audio file fail");
                return;
            }
            SoundMessageContent soundContent = new SoundMessageContent(file.getAbsolutePath());
            soundContent.setDuration(duration);
            sendMessage(soundContent);
        }
    }

    private void playAudio(UiMessage message, File file) {
        Uri uri = Uri.fromFile(file);
        AudioPlayManager.getInstance().startPlay(MyApp.getContext(), uri, new IAudioPlayListener() {
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
                    toPlayAudioMessage = null;
                    postMessageUpdate(message);
                }
            }

            @Override
            public void onComplete(Uri var1) {
                if (uri.equals(var1)) {
                    message.isPlaying = false;
                    toPlayAudioMessage = null;
                    postMessageUpdate(message);
                }
            }
        });
    }

    public File mediaMessageContentFile(UiMessage message) {

        String dir = null;
        String name = null;
        MessageContent content = message.message.content;
        if (!(content instanceof MediaMessageContent)) {
            return null;
        }
        if (!TextUtils.isEmpty(((MediaMessageContent) content).localPath)) {
            return new File(((MediaMessageContent) content).localPath);
        }

        switch (((MediaMessageContent) content).mediaType) {
            case VOICE:
                name = message.message.messageUid + ".mp3";
                dir = Config.AUDIO_SAVE_DIR;
                break;
            case FILE:
                name = message.message.messageUid + "-" + ((FileMessageContent) message.message.content).getName();
                dir = Config.FILE_SAVE_DIR;
                break;
            case VIDEO:
                name = message.message.messageUid + ".mp4";
                dir = Config.VIDEO_SAVE_DIR;
                break;
            default:
        }
        if (TextUtils.isEmpty(dir) || TextUtils.isEmpty(name)) {
            return null;
        }
        return new File(dir, name);
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

        DownloadManager.get().download(((MediaMessageContent) content).remoteUrl, targetFile.getParent(), targetFile.getName() + ".tmp", new DownloadManager.OnDownloadListener() {
            @Override
            public void onSuccess(File file) {
                file.renameTo(targetFile);

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

    private void postNewMessage(UiMessage message) {
        if (message == null || message.message == null) {
            return;
        }
        if (messageLiveData != null) {
            UIUtils.postTaskSafely(() -> messageLiveData.setValue(message));
        }
    }

    @Override
    public void onSendSuccess(Message message) {
        if (isMessageInCurrentConversation(message)) {
            postMessageUpdate(new UiMessage(message));
        }
    }

    @Override
    public void onSendFail(Message message, int errorCode) {
        if (isMessageInCurrentConversation(message)) {
            postMessageUpdate(new UiMessage(message));
        }
    }

    @Override
    public void onSendPrepare(Message message, long savedTime) {
        if (isMessageInCurrentConversation(message)) {
            postNewMessage(new UiMessage(message));
        }
    }

    @Override
    public void onProgress(Message message, long uploaded, long total) {
        if (isMessageInCurrentConversation(message)) {
            UiMessage uiMessage = new UiMessage(message);
            uiMessage.progress = (int) (uploaded * 100 / total);
            postMessageUpdate(uiMessage);
        }
    }

    @Override
    public void onMediaUpload(Message message, String remoteUrl) {
        if (mediaUploadedLiveData != null) {
            if (isMessageInCurrentConversation(message)) {
                Map<String, String> map = new HashMap<>();
                map.put(((MediaMessageContent) message.content).localPath, remoteUrl);

                UIUtils.postTaskSafely(() -> mediaUploadedLiveData.setValue(map));
            }
        }
    }

    @Override
    public void onMessageUpdate(Message message) {
        if (isMessageInCurrentConversation(message)) {
            postNewMessage(new UiMessage(message));
        }
    }

    private boolean isMessageInCurrentConversation(Message message) {
        return message.conversation.equals(conversation);
    }

    @Override
    public void onClearMessage(Conversation conversation) {
        if (clearMessageLiveData != null) {
            clearMessageLiveData.postValue(new Object());
        }
    }
}
