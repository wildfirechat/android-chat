package cn.wildfire.chat.kit.conversation.message.model;

import cn.wildfirechat.message.Message;

public class UiMessage {
    public boolean isPlaying;
    public boolean isDownloading;
    public boolean isFocus;
    public boolean isChecked;
    /**
     * media类型消息，上传或下载的进度
     */
    public int progress;
    public Message message;

    public UiMessage(Message message) {
        this.message = message;
    }

}
