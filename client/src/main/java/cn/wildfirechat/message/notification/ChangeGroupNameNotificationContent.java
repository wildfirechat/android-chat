package cn.wildfirechat.message.notification;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_CHANGE_GROUP_NAME;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_CHANGE_GROUP_NAME, flag = PersistFlag.Persist)
public class ChangeGroupNameNotificationContent extends NotificationMessageContent {
    public String operateUser;
    public String name;

    public ChangeGroupNameNotificationContent() {
    }

    @Override
    public String formatNotification() {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您");
        } else {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(operateUser, false);
            sb.append(userInfo.displayName);
        }
        sb.append("修改群名为");
        sb.append(name);

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("o", operateUser);
            objWrite.put("n", name);

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
                operateUser = jsonObject.optString("o");
                name = jsonObject.optString("n");
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
        dest.writeString(this.operateUser);
        dest.writeString(this.name);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
    }

    protected ChangeGroupNameNotificationContent(Parcel in) {
        this.operateUser = in.readString();
        this.name = in.readString();
        this.fromSelf = in.readByte() != 0;
    }

    public static final Parcelable.Creator<ChangeGroupNameNotificationContent> CREATOR = new Parcelable.Creator<ChangeGroupNameNotificationContent>() {
        @Override
        public ChangeGroupNameNotificationContent createFromParcel(Parcel source) {
            return new ChangeGroupNameNotificationContent(source);
        }

        @Override
        public ChangeGroupNameNotificationContent[] newArray(int size) {
            return new ChangeGroupNameNotificationContent[size];
        }
    };
}
