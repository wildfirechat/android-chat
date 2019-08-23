package cn.wildfirechat.message;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Typing;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Typing, flag = PersistFlag.Transparent)
public class TypingMessageContent extends MessageContent {
    public static final int TYPING_TEXT = 0;
    public static final int TYPING_VOICE = 1;
    public static final int TYPING_CAMERA = 2;
    public static final int TYPING_LOCATION = 3;
    public static final int TYPING_FILE = 4;

    private int typingType;

    public TypingMessageContent() {
    }

    public TypingMessageContent(int typingType) {
        this.typingType = typingType;
    }

    public int getTypingType() {
        return typingType;
    }

    public void setTypingType(int typingType) {
        this.typingType = typingType;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.content = typingType + "";
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        typingType = Integer.parseInt(payload.content);
    }

    @Override
    public String digest(Message message) {
        return "";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.typingType);
    }

    protected TypingMessageContent(Parcel in) {
        super(in);
        this.typingType = in.readInt();
    }

    public static final Creator<TypingMessageContent> CREATOR = new Creator<TypingMessageContent>() {
        @Override
        public TypingMessageContent createFromParcel(Parcel source) {
            return new TypingMessageContent(source);
        }

        @Override
        public TypingMessageContent[] newArray(int size) {
            return new TypingMessageContent[size];
        }
    };
}
