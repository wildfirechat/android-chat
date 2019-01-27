package cn.wildfirechat.message.notification;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_TRANSFER_GROUP_OWNER;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_TRANSFER_GROUP_OWNER, flag = PersistFlag.Persist)
public class TransferGroupOwnerNotificationContent extends NotificationMessageContent {
    public String operator;
    public String newOwner;

    public TransferGroupOwnerNotificationContent() {
    }

    @Override
    public String formatNotification() {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您把群组转让给了");
        } else {
            sb.append(operator);
            sb.append("把群组转让给了");
        }
        sb.append(newOwner);

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("o", operator);
            objWrite.put("m", newOwner);
            payload.content = objWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void decode(MessagePayload payload) {
        try {
            if (payload.content != null) {
                JSONObject jsonObject = new JSONObject(payload.content);
                operator = jsonObject.optString("o");
                newOwner = jsonObject.optString("m");
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
        dest.writeString(this.newOwner);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
    }

    protected TransferGroupOwnerNotificationContent(Parcel in) {
        this.operator = in.readString();
        this.newOwner = in.readString();
        this.fromSelf = in.readByte() != 0;
    }

    public static final Creator<TransferGroupOwnerNotificationContent> CREATOR = new Creator<TransferGroupOwnerNotificationContent>() {
        @Override
        public TransferGroupOwnerNotificationContent createFromParcel(Parcel source) {
            return new TransferGroupOwnerNotificationContent(source);
        }

        @Override
        public TransferGroupOwnerNotificationContent[] newArray(int size) {
            return new TransferGroupOwnerNotificationContent[size];
        }
    };
}
