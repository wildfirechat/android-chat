package cn.wildfirechat.message.notification;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Friend_Greeting;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Tip_Notification;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_Friend_Greeting, flag = PersistFlag.Persist)
public class FriendGreetingMessageContent extends NotificationMessageContent {
    public FriendGreetingMessageContent() {
    }


    @Override
    public String formatNotification(Message message) {
        return "以上是打招呼的内容";
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

    protected FriendGreetingMessageContent(Parcel in) {
        super(in);
    }

    public static final Creator<FriendGreetingMessageContent> CREATOR = new Creator<FriendGreetingMessageContent>() {
        @Override
        public FriendGreetingMessageContent createFromParcel(Parcel source) {
            return new FriendGreetingMessageContent(source);
        }

        @Override
        public FriendGreetingMessageContent[] newArray(int size) {
            return new FriendGreetingMessageContent[size];
        }
    };
}
