package cn.wildfire.chat.kit.viewmodel;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
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
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnClearMessageListener;
import cn.wildfirechat.remote.OnDeleteMessageListener;
import cn.wildfirechat.remote.OnMessageUpdateListener;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.OnSendMessageListener;

public class MessageViewModel extends ViewModel implements OnReceiveMessageListener,
    OnSendMessageListener,
    OnDeleteMessageListener,
    OnRecallMessageListener,
    OnMessageUpdateListener,
    OnClearMessageListener {
    private MutableLiveData<UiMessage> messageLiveData;
    private MutableLiveData<UiMessage> messageUpdateLiveData;
    private MutableLiveData<UiMessage> messageRemovedLiveData;
    private MutableLiveData<Map<String, String>> mediaUploadedLiveData;
    private MutableLiveData<Object> clearMessageLiveData;

    private Message toPlayAudioMessage;

    public MessageViewModel() {
        ChatManager.Instance().addOnReceiveMessageListener(this);
        ChatManager.Instance().addRecallMessageListener(this);
        ChatManager.Instance().addSendMessageListener(this);
        ChatManager.Instance().addOnMessageUpdateListener(this);
        ChatManager.Instance().addClearMessageListener(this);
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
                postNewMessage(new UiMessage(msg));
            }
        }
    }

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


    @Override
    public void onRecallMessage(Message message) {
        if (message != null) {
            UiMessage uiMessage = new UiMessage(message);
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
                Message msg = ChatManager.Instance().getMessage(message.messageId);
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
     * 社区版IM Server执行本地删除，社区版不支持远程删除
     * 商业版IM Server执行远程删除
     * @param message
     */
    public void deleteMessage(Message message) {
        if (ChatManager.Instance().isCommercialServer()) {
            ChatManager.Instance().deleteMessage(message, new GeneralCallback() {
                @Override
                public void onSuccess() {
                    if (messageRemovedLiveData != null) {
                        messageRemovedLiveData.setValue(new UiMessage(message));
                    }
                }

                @Override
                public void onFail(int errorCode) {
                    Log.e("deleteMessage", "delete message error " + errorCode);
                }
            });

        } else {
            if (messageRemovedLiveData != null) {
                messageRemovedLiveData.setValue(new UiMessage(message));
            }
            ChatManager.Instance().deleteMessage(message);
        }
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

    public void stopPlayAudio() {
        AudioPlayManager.getInstance().stopPlay();
    }

    public void sendMessage(Conversation conversation, List<String> toUsers, MessageContent content) {

    }

    public void sendMessage(Conversation conversation, MessageContent content) {
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

    public void sendTextMsg(Conversation conversation, TextMessageContent txtContent) {
        txtContent.extra = "hello extra";
        sendMessage(conversation, txtContent);
        ChatManager.Instance().setConversationDraft(conversation, null);
    }

    public void saveDraft(Conversation conversation, String draftString) {
        ChatManager.Instance().setConversationDraft(conversation, draftString);
    }

    public void setConversationSilent(Conversation conversation, boolean silent) {
        ChatManager.Instance().setConversationSilent(conversation, silent);
    }

    public void sendImgMsg(Conversation conversation, Uri imageFileThumbUri, Uri imageFileSourceUri) {
        ImageMessageContent imgContent = new ImageMessageContent(imageFileSourceUri.getEncodedPath());
        String thumbParam = ChatManager.Instance().getImageThumbPara();
        if (!TextUtils.isEmpty(thumbParam)) {
            imgContent.setThumbPara(ChatManager.Instance().getImageThumbPara());
        }
        sendMessage(conversation, imgContent);
    }

    public void sendImgMsg(Conversation conversation, File imageFileThumb, File imageFileSource) {
        // Uri.fromFile()遇到中文檔名會轉 ASCII，這個 ASCII 的 path 將導致後面 ChatManager.sendMessage()
        // 在 new File()時找不到 File 而 return
        Uri imageFileThumbUri = Uri.parse(Uri.decode(imageFileThumb.getAbsolutePath()));
//        Uri imageFileThumbUri = Uri.fromFile(imageFileThumb);
        Uri imageFileSourceUri = Uri.parse(Uri.decode(imageFileSource.getAbsolutePath()));
//        Uri imageFileSourceUri = Uri.fromFile(imageFileSource);
        sendImgMsg(conversation, imageFileThumbUri, imageFileSourceUri);

    }

    public void sendVideoMsg(Conversation conversation, File file) {
        VideoMessageContent videoMessageContent = new VideoMessageContent(file.getPath());
        sendMessage(conversation, videoMessageContent);
    }

    public void sendStickerMsg(Conversation conversation, String localPath, String remoteUrl) {
        StickerMessageContent stickerMessageContent = new StickerMessageContent(localPath);
        stickerMessageContent.remoteUrl = remoteUrl;
        sendMessage(conversation, stickerMessageContent);
    }

    public void sendFileMsg(Conversation conversation, File file) {
        FileMessageContent fileMessageContent = new FileMessageContent(file.getPath());
        sendMessage(conversation, fileMessageContent);
    }

    public void sendLocationMessage(Conversation conversation, LocationData locationData) {
        LocationMessageContent locCont = new LocationMessageContent();
        locCont.setTitle(locationData.getPoi());
        locCont.getLocation().setLatitude(locationData.getLat());
        locCont.getLocation().setLongitude(locationData.getLng());
        locCont.setThumbnail(locationData.getThumbnail());

        sendMessage(conversation, locCont);
    }

    public void sendAudioFile(Conversation conversation, Uri audioPath, int duration) {
        if (audioPath != null) {
            File file = new File(audioPath.getPath());
            if (!file.exists() || file.length() == 0L) {
                Log.e("ConversationViewModel", "send audio file fail");
                return;
            }
            SoundMessageContent soundContent = new SoundMessageContent(file.getAbsolutePath());
            soundContent.setDuration(duration);
            sendMessage(conversation, soundContent);
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

        DownloadManager.download(((MediaMessageContent) content).remoteUrl, targetFile.getParent(), targetFile.getName() + ".tmp", new DownloadManager.OnDownloadListener() {
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
        postMessageUpdate(new UiMessage(message));
    }

    @Override
    public void onSendFail(Message message, int errorCode) {
        postMessageUpdate(new UiMessage(message));
    }

    @Override
    public void onSendPrepare(Message message, long savedTime) {
        postNewMessage(new UiMessage(message));
    }

    @Override
    public void onProgress(Message message, long uploaded, long total) {
        UiMessage uiMessage = new UiMessage(message);
        uiMessage.progress = (int) (uploaded * 100 / total);
        postMessageUpdate(uiMessage);
    }

    @Override
    public void onMediaUpload(Message message, String remoteUrl) {
        if (mediaUploadedLiveData != null) {
            Map<String, String> map = new HashMap<>();
            map.put(((MediaMessageContent) message.content).localPath, remoteUrl);
            UIUtils.postTaskSafely(() -> mediaUploadedLiveData.setValue(map));
        }
    }

    @Override
    public void onMessageUpdate(Message message) {
        postNewMessage(new UiMessage(message));
    }

    @Override
    public void onClearMessage(Conversation conversation) {
        if (clearMessageLiveData != null) {
            clearMessageLiveData.postValue(new Object());
        }
    }
}
