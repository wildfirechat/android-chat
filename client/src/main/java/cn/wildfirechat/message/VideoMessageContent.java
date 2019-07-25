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
    private byte[] thumbnailBytes;

    // 所有消息都需要一个默认构造函数
    public VideoMessageContent() {
    }

    public VideoMessageContent(String videoPath) {
        this.localPath = videoPath;

        this.thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MICRO_KIND);
        this.thumbnail = ThumbnailUtils.extractThumbnail(this.thumbnail, 320, 240,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        this.mediaType = MessageContentMediaType.VIDEO;
    }

    public Bitmap getThumbnail() {
        if (thumbnailBytes != null) {
            thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length);
        }
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }


    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = "[视频]";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        payload.binaryContent = baos.toByteArray();
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        thumbnailBytes = payload.binaryContent;
    }

    @Override
    public String digest(Message message) {
        return "[视频]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.thumbnail, flags);
        dest.writeByteArray(this.thumbnailBytes);
        dest.writeString(this.localPath);
        dest.writeString(this.remoteUrl);
        dest.writeInt(this.mediaType == null ? -1 : this.mediaType.ordinal());
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
    }

    protected VideoMessageContent(Parcel in) {
        this.thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
        this.thumbnailBytes = in.createByteArray();
        this.localPath = in.readString();
        this.remoteUrl = in.readString();
        int tmpMediaType = in.readInt();
        this.mediaType = tmpMediaType == -1 ? null : MessageContentMediaType.values()[tmpMediaType];
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
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
