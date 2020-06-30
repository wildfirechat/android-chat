package cn.wildfirechat.message.notification;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Friend_Added;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Friend_Greeting;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_Friend_Added, flag = PersistFlag.Persist)
public class FriendAddedMessageContent extends NotificationMessageContent {
    public FriendAddedMessageContent() {
    }


    @Override
    public String formatNotification(Message message) {
        return "你们已经是好友了，可以开始聊天了。";
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    protected FriendAddedMessageContent(Parcel in) {
        super(in);
    }

    public static final Creator<FriendAddedMessageContent> CREATOR = new Creator<FriendAddedMessageContent>() {
        @Override
        public FriendAddedMessageContent createFromParcel(Parcel source) {
            return new FriendAddedMessageContent(source);
        }

        @Override
        public FriendAddedMessageContent[] newArray(int size) {
            return new FriendAddedMessageContent[size];
        }
    };
}
