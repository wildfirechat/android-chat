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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_CHANGE_GROUP_PORTRAIT;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_CHANGE_GROUP_PORTRAIT, flag = PersistFlag.Persist)
public class ChangeGroupPortraitNotificationContent extends NotificationMessageContent {
    public String operateUser;

    public ChangeGroupPortraitNotificationContent() {
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
        sb.append("更新了群头像");

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("o", operateUser);
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
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
    }

    protected ChangeGroupPortraitNotificationContent(Parcel in) {
        this.operateUser = in.readString();
        this.fromSelf = in.readByte() != 0;
    }

    public static final Parcelable.Creator<ChangeGroupPortraitNotificationContent> CREATOR = new Parcelable.Creator<ChangeGroupPortraitNotificationContent>() {
        @Override
        public ChangeGroupPortraitNotificationContent createFromParcel(Parcel source) {
            return new ChangeGroupPortraitNotificationContent(source);
        }

        @Override
        public ChangeGroupPortraitNotificationContent[] newArray(int size) {
            return new ChangeGroupPortraitNotificationContent[size];
        }
    };
}
