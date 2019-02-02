package cn.wildfire.chat.kit.conversation;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import cn.wildfire.chat.app.MyApp;
import cn.wildfire.chat.kit.audio.AudioPlayManager;
import cn.wildfire.chat.kit.audio.IAudioPlayListener;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.message.viewholder.AudioMessageContentViewHolder;
import cn.wildfire.chat.kit.third.location.data.LocationData;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadUtil;
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
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnMessageUpdateListener;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.OnSendMessageListener;

public class ConversationViewModel extends ViewModel implements OnReceiveMessageListener,
        OnSendMessageListener,
        OnRecallMessageListener, OnMessageUpdateListener {
    private MutableLiveData<UiMessage> messageLiveData;
    private MutableLiveData<UiMessage> messageUpdateLiveData;
    private MutableLiveData<UiMessage> messageRemovedLiveData;
    private MutableLiveData<Map<String, String>> mediaUploadedLiveData;
    private Conversation conversation;
    // channel主发起和某个channel听众的私聊会话时，才有效
    private String channelPrivateChatUser;

    private Message toPlayAudioMessage;

    private static final String audioDir = "audio";
    private static final String videoDir = "video";

    public ConversationViewModel(Conversation conversation, String channelPrivateChatUser) {
        this.conversation = conversation;
        this.channelPrivateChatUser = channelPrivateChatUser;
        ChatManager.Instance().addOnReceiveMessageListener(this);
        ChatManager.Instance().addSendMessageListener(this);
        ChatManager.Instance().addOnMessageUpdateListener(this);
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
        ChatManager.Instance().removeSendMessageListener(this);
        ChatManager.Instance().removeOnMessageUpdateListener(this);
    }

    @Override
    public void onReceive(List<Message> messages, boolean hasMore) {
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

    public MutableLiveData<List<UiMessage>> loadOldMessages(long fromIndex, int count) {
        MutableLiveData<List<UiMessage>> result = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<Message> messageList = ChatManager.Instance().getMessages(conversation, fromIndex, true, count, channelPrivateChatUser);
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
        return loadOldMessages(0, 20);
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
            public void onFailure(int errorCode) {
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
        // TODO
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
        AudioPlayManager.getInstance().stopPlay();

        toPlayAudioMessage = message.message;
        SoundMessageContent content = (SoundMessageContent) message.message.content;

        String path = null;
        if (!TextUtils.isEmpty(content.localPath)) {
            path = content.localPath;
        } else {
            File file = new File(Environment.getExternalStorageDirectory(), audioDir + File.separator + message.message.messageUid + "");
            if (file.exists()) {
                path = file.getAbsolutePath();
            }
        }

        if (path != null) {
            playAudio(message, path);
        } else {
            downloadAudio(message);
        }
    }


    public void sendMessage(MessageContent content) {
        Message msg = new Message();
        msg.conversation = conversation;
        msg.content = content;
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
        ImageMessageContent imgContent = new ImageMessageContent();
        imgContent.setThumbnail(BitmapFactory.decodeFile(imageFileThumbUri.getEncodedPath()));
        imgContent.localPath = imageFileSourceUri.getEncodedPath();
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
            SoundMessageContent soundContent = new SoundMessageContent();
            soundContent.setDuration(duration);
            soundContent.localPath = file.getAbsolutePath();
            sendMessage(soundContent);
        }
    }

    private void playAudio(UiMessage message, String audioPath) {
        Uri uri = Uri.parse(audioPath);
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

    private void downloadAudio(UiMessage message) {
        SoundMessageContent content = (SoundMessageContent) message.message.content;
        DownloadUtil.get().download(content.remoteUrl, audioDir, message.message.messageUid + "", new DownloadUtil.OnDownloadListener() {
            @Override
            public void onSuccess(String fileName) {
                message.isDownloading = false;
                message.progress = 100;
                postMessageUpdate(message);

                playAudio(message, new File(Environment.getExternalStorageDirectory(), audioDir + File.separator + message.message.messageUid).getAbsolutePath());
            }

            @Override
            public void onDownloading(int progress) {
                message.isDownloading = true;
                message.progress = progress;
                postMessageUpdate(message);
            }

            @Override
            public void onDownloadFailed() {
                message.isDownloading = false;
                message.progress = 0;
                postMessageUpdate(message);
                Log.e(AudioMessageContentViewHolder.class.getSimpleName(), "download failed: " + message.message.messageId);
            }
        });
    }

    private void postMessageUpdate(UiMessage message) {
        if (messageUpdateLiveData != null) {
            UIUtils.postTaskSafely(() -> messageUpdateLiveData.setValue(message));
        }
    }

    private void postNewMessage(UiMessage message) {
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
    public void onSendFailure(Message message, int errorCode) {
        if (isMessageInCurrentConversation(message)) {
            postMessageUpdate(new UiMessage(message));
        }
    }

    @Override
    public void onSendPrepared(Message message, long savedTime) {
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
    public void onMediaUploaded(Message message, String remoteUrl) {
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
        if (!message.conversation.equals(conversation)) {
            return false;
        }
        if (message.conversation.type == Conversation.ConversationType.Channel && !TextUtils.isEmpty(channelPrivateChatUser)) {
            if (message.direction == MessageDirection.Receive && message.sender.equals(channelPrivateChatUser)) {
                return true;
            } else if (message.direction == MessageDirection.Send && (message.to == null || message.to.equals(channelPrivateChatUser))) {
                return true;
            } else {
                return false;
            }

        } else {
            return true;
        }
    }
}
