/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

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
    // 语音消息 speechToText
    public String audioMessageSpeechToText;
    public Object extra;

    public boolean continuousPlayAudio;
    public boolean audioPlayCompleted;

    public UiMessage(Message message) {
        this.message = message;
    }

    public UiMessage(Message message, Object extra) {
        this.message = message;
        this.extra = extra;
    }
}
