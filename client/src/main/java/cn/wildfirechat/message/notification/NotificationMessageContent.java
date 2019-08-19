package cn.wildfirechat.message.notification;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;

/**
 * Created by heavyrainlee on 19/12/2017.
 */


public abstract class NotificationMessageContent extends MessageContent {
    /**
     * 是否是自己发送的
     * <p>
     * 用户可以不用设置这个值，client会自动置上
     */
    public boolean fromSelf;

    public abstract String formatNotification(Message message);

    @Override
    public String digest(Message message) {
        return formatNotification(message);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
    }

    public NotificationMessageContent() {
    }

    protected NotificationMessageContent(Parcel in) {
        super(in);
        this.fromSelf = in.readByte() != 0;
    }
}
