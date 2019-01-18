package cn.wildfirechat.message;

import android.os.Parcel;

import java.io.File;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_File;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_File, flag = PersistFlag.Persist_And_Count)
public class FileMessageContent extends MediaMessageContent {
    private String name;
    private int size;


    public FileMessageContent() {
    }

    public FileMessageContent(String filePath) {
        File file = new File(filePath);
        this.name = filePath.substring(filePath.lastIndexOf("/") + 1);
        this.size = (int) file.length();
        this.localPath = filePath;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.searchableContent = name;
        payload.mediaType = MessageContentMediaType.FILE;
        payload.content = size + "";
        payload.localMediaPath = localPath;
        payload.remoteMediaUrl = remoteUrl;

        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        name = payload.searchableContent;
        size = Integer.parseInt(payload.content);
        remoteUrl = payload.remoteMediaUrl;
        localPath = payload.localMediaPath;
    }

    @Override
    public String digest() {
        return "[文件]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(size);
        dest.writeString(this.localPath);
        dest.writeString(this.remoteUrl);
    }

    protected FileMessageContent(Parcel in) {
        this.name = in.readString();
        this.size = in.readInt();
        this.localPath = in.readString();
        this.remoteUrl = in.readString();
    }

    public static final Creator<FileMessageContent> CREATOR = new Creator<FileMessageContent>() {
        @Override
        public FileMessageContent createFromParcel(Parcel source) {
            return new FileMessageContent(source);
        }

        @Override
        public FileMessageContent[] newArray(int size) {
            return new FileMessageContent[size];
        }
    };
}
