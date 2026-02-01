/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import cn.wildfirechat.client.ClientService;
import cn.wildfirechat.message.core.MessagePayload;

/**
 * 媒体消息内容基类
 * <p>
 * 用于所有包含媒体文件的消息类型，如图片、语音、视频、文件等。
 * 提供媒体文件的本地路径和远程URL的统一管理。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public abstract class MediaMessageContent extends MessageContent {
    /**
     * 媒体文件的本地路径
     */
    public String localPath;

    /**
     * 媒体文件的远程URL
     */
    public String remoteUrl;

    /**
     * 媒体类型（图片、语音、视频、文件等）
     */
    public MessageContentMediaType mediaType;

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.localMediaPath = this.localPath;
        payload.remoteMediaUrl = this.remoteUrl;
        payload.mediaType = mediaType;
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        this.localPath = payload.localMediaPath;
        this.remoteUrl = ClientService.urlRedirect(payload.remoteMediaUrl);
        this.mediaType = payload.mediaType;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.localPath);
        dest.writeString(this.remoteUrl);
        dest.writeInt(this.mediaType == null ? -1 : this.mediaType.ordinal());
    }

    public MediaMessageContent() {
    }

    protected MediaMessageContent(Parcel in) {
        super(in);
        this.localPath = in.readString();
        this.remoteUrl = in.readString();
        int tmpMediaType = in.readInt();
        this.mediaType = tmpMediaType == -1 ? null : MessageContentMediaType.values()[tmpMediaType];
    }
}
