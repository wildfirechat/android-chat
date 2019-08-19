package cn.wildfirechat.message;

import android.os.Parcel;

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
