package cn.wildfirechat.message;

import android.os.Parcel;
import android.text.TextUtils;

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
    private static final String FILE_NAME_PREFIX = "[文件] ";

    public FileMessageContent() {
    }

    public FileMessageContent(String filePath) {
        File file = new File(filePath);
        this.name = filePath.substring(filePath.lastIndexOf("/") + 1);
        this.size = (int) file.length();
        this.localPath = filePath;
        this.mediaType = MessageContentMediaType.FILE;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = FILE_NAME_PREFIX + name;
        payload.content = size + "";

        return payload;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        if (TextUtils.isEmpty(payload.searchableContent)) {
            return;
        }
        if (payload.searchableContent.startsWith(FILE_NAME_PREFIX)) {
            name = payload.searchableContent.substring(payload.searchableContent.indexOf(FILE_NAME_PREFIX) + FILE_NAME_PREFIX.length());
        } else {
            name = payload.searchableContent;
        }
        size = Integer.parseInt(payload.content);
    }

    @Override
    public String digest(Message message) {
        return "[文件]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.name);
        dest.writeInt(this.size);
    }

    protected FileMessageContent(Parcel in) {
        super(in);
        this.name = in.readString();
        this.size = in.readInt();
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
