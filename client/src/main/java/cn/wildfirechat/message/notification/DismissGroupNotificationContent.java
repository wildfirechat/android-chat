package cn.wildfirechat.message.notification;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_DISMISS_GROUP;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_DISMISS_GROUP, flag = PersistFlag.Persist)
public class DismissGroupNotificationContent extends NotificationMessageContent {
    public String operator;

    public DismissGroupNotificationContent() {
    }

    @Override
    public String formatNotification() {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您解散了群组 ");
        } else {
            sb.append(operator);
            sb.append("解散了群组 ");
        }

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("o", operator);
            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                operator = jsonObject.optString("o");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest() {
        return formatNotification();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.operator);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
    }

    protected DismissGroupNotificationContent(Parcel in) {
        this.operator = in.readString();
        this.fromSelf = in.readByte() != 0;
    }

    public static final Parcelable.Creator<DismissGroupNotificationContent> CREATOR = new Parcelable.Creator<DismissGroupNotificationContent>() {
        @Override
        public DismissGroupNotificationContent createFromParcel(Parcel source) {
            return new DismissGroupNotificationContent(source);
        }

        @Override
        public DismissGroupNotificationContent[] newArray(int size) {
            return new DismissGroupNotificationContent[size];
        }
    };
}
