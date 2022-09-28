/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mm;

import android.graphics.Bitmap;

import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.model.Conversation;

public class MediaEntry {
    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_VIDEO = 1;
    private int type;
    private String mediaUrl;
    private String mediaLocalPath;
    private String thumbnailUrl;
    // TODO 消息里的缩略图会被移除
    private Bitmap thumbnail;
    private Message message;

    public MediaEntry() {
    }

    public MediaEntry(Message message) {
        this.message = message;
        MediaMessageContent content = (MediaMessageContent) message.content;
        this.mediaUrl = content.remoteUrl;
        if (message.conversation.type == Conversation.ConversationType.SecretChat) {
            this.mediaUrl = DownloadManager.buildSecretChatMediaUrl(message);
        }
        this.mediaLocalPath = content.localPath;
        if (content instanceof ImageMessageContent) {
            this.type = TYPE_IMAGE;
            this.thumbnail = ((ImageMessageContent) content).getThumbnail();
        } else if (content instanceof VideoMessageContent) {
            this.type = TYPE_VIDEO;
            this.thumbnail = ((VideoMessageContent) content).getThumbnail();
        }
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaLocalPath() {
        return mediaLocalPath;
    }

    public void setMediaLocalPath(String mediaLocalPath) {
        this.mediaLocalPath = mediaLocalPath;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
