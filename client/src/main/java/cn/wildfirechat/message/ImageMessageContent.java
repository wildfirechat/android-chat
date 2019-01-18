package cn.wildfirechat.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;

import java.io.ByteArrayOutputStream;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Image, flag = PersistFlag.Persist_And_Count)
public class ImageMessageContent extends MediaMessageContent {
    private Bitmap thumbnail;


    public ImageMessageContent() {
    }

    public ImageMessageContent(String content) {

    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }


    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.searchableContent = "[图片]";
        payload.mediaType = MessageContentMediaType.IMAGE;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        payload.binaryContent = baos.toByteArray();
        payload.remoteMediaUrl = remoteUrl;
        payload.localMediaPath = localPath;
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        if (payload.binaryContent != null) {
            thumbnail = BitmapFactory.decodeByteArray(payload.binaryContent, 0, payload.binaryContent.length);
        }
        remoteUrl = payload.remoteMediaUrl;
        localPath = payload.localMediaPath;
    }

    @Override
    public String digest() {
        return "[图片]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.thumbnail, flags);
        dest.writeString(this.localPath);
        dest.writeString(this.remoteUrl);
    }

    protected ImageMessageContent(Parcel in) {
        this.thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
        this.localPath = in.readString();
        this.remoteUrl = in.readString();
    }

    public static final Creator<ImageMessageContent> CREATOR = new Creator<ImageMessageContent>() {
        @Override
        public ImageMessageContent createFromParcel(Parcel source) {
            return new ImageMessageContent(source);
        }

        @Override
        public ImageMessageContent[] newArray(int size) {
            return new ImageMessageContent[size];
        }
    };
}
