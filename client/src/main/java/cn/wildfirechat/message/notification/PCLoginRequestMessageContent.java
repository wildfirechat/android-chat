package cn.wildfirechat.message.notification;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

@ContentTag(type = MessageContentType.ContentType_PC_LOGIN_REQUSET, flag = PersistFlag.Transparent)
public class PCLoginRequestMessageContent extends NotificationMessageContent {
    // 3 windows, 4 osx, 5 web
    private int platform;
    private String token;

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String formatNotification(Message message) {
        return null;
    }

    @Override
    public MessagePayload encode() {
        // never
        return null;
    }

    @Override
    public void decode(MessagePayload payload) {
        try {
            JSONObject obj = new JSONObject(new String(payload.binaryContent));
            platform = obj.optInt("p");
            token = obj.optString("t");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.platform);
        dest.writeString(this.token);
    }

    public PCLoginRequestMessageContent() {
    }

    protected PCLoginRequestMessageContent(Parcel in) {
        super(in);
        this.platform = in.readInt();
        this.token = in.readString();
    }

    public static final Creator<PCLoginRequestMessageContent> CREATOR = new Creator<PCLoginRequestMessageContent>() {
        @Override
        public PCLoginRequestMessageContent createFromParcel(Parcel source) {
            return new PCLoginRequestMessageContent(source);
        }

        @Override
        public PCLoginRequestMessageContent[] newArray(int size) {
            return new PCLoginRequestMessageContent[size];
        }
    };
}
