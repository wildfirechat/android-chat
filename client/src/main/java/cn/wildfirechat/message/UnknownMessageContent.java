package cn.wildfirechat.message;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Unknown;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Unknown, flag = PersistFlag.Persist)
public class UnknownMessageContent extends MessageContent {
    private MessagePayload orignalPayload;

    public UnknownMessageContent() {
    }

    public MessagePayload getOrignalPayload() {
        return orignalPayload;
    }

    public void setOrignalPayload(MessagePayload payload) {
        this.orignalPayload = payload;
    }

    @Override
    public MessagePayload encode() {
        return orignalPayload;
    }


    @Override
    public void decode(MessagePayload payload) {
        orignalPayload = payload;
    }

    @Override
    public String digest(Message message) {
        return "未知类型消息(" + (orignalPayload != null ? orignalPayload.contentType : "") + ")";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(this.orignalPayload, flags);
    }

    protected UnknownMessageContent(Parcel in) {
        super(in);
        this.orignalPayload = in.readParcelable(MessagePayload.class.getClassLoader());
    }

    public static final Creator<UnknownMessageContent> CREATOR = new Creator<UnknownMessageContent>() {
        @Override
        public UnknownMessageContent createFromParcel(Parcel source) {
            return new UnknownMessageContent(source);
        }

        @Override
        public UnknownMessageContent[] newArray(int size) {
            return new UnknownMessageContent[size];
        }
    };
}
