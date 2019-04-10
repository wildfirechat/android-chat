package cn.wildfirechat.message;

import cn.wildfirechat.message.core.MessagePayload;

/**
 * Created by heavyrainlee on 19/12/2017.
 */

public abstract class MediaMessageContent extends MessageContent {
    public String localPath;
    public String remoteUrl;
    public MessageContentMediaType mediaType;

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.localMediaPath = this.localPath;
        payload.remoteMediaUrl = this.remoteUrl;
        payload.mediaType = mediaType;
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        this.localPath = payload.localMediaPath;
        this.remoteUrl = payload.remoteMediaUrl;
        this.mediaType = payload.mediaType;
    }
}
