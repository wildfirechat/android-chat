package cn.wildfirechat.message.notification;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Tip_Notification;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_Tip_Notification, flag = PersistFlag.Persist)
public class TipNotificationContent extends NotificationMessageContent {
    public String tip;

    public TipNotificationContent() {
    }


    @Override
    public String formatNotification(Message message) {
        return tip;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.content = tip;

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        tip = payload.content;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.tip);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
    }

    protected TipNotificationContent(Parcel in) {
        this.tip = in.readString();
        this.fromSelf = in.readByte() != 0;
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
    }

    public static final Creator<TipNotificationContent> CREATOR = new Creator<TipNotificationContent>() {
        @Override
        public TipNotificationContent createFromParcel(Parcel source) {
            return new TipNotificationContent(source);
        }

        @Override
        public TipNotificationContent[] newArray(int size) {
            return new TipNotificationContent[size];
        }
    };
}
