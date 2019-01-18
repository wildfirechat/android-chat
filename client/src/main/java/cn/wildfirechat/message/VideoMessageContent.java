package cn.wildfirechat.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Parcel;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Video, flag = PersistFlag.Persist_And_Count)
public class VideoMessageContent extends MediaMessageContent {
    private Bitmap thumbnail;


    public VideoMessageContent() {
    }

    public VideoMessageContent(String path) {
        this.localPath = path;

        this.thumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
        this.thumbnail = ThumbnailUtils.extractThumbnail(this.thumbnail, 240, 240,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
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
        payload.searchableContent = "[视频]";
        payload.mediaType = MessageContentMediaType.VIDEO;
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
        return "[视频]";
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

    protected VideoMessageContent(Parcel in) {
        this.thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
        this.localPath = in.readString();
        this.remoteUrl = in.readString();
    }

    public static final Creator<VideoMessageContent> CREATOR = new Creator<VideoMessageContent>() {
        @Override
        public VideoMessageContent createFromParcel(Parcel source) {
            return new VideoMessageContent(source);
        }

        @Override
        public VideoMessageContent[] newArray(int size) {
            return new VideoMessageContent[size];
        }
    };
}
