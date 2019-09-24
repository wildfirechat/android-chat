package cn.wildfire.chat.kit.conversation.message.model;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UiMessage uiMessage = (UiMessage) o;
        return Conversation.equals(message, uiMessage.message);
    }

    @Override
    public int hashCode() {

        return Conversation.hashCode(message);
    }
}
