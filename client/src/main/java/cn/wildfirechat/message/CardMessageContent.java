package cn.wildfirechat.message;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Card;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Card, flag = PersistFlag.Persist_And_Count)
public class CardMessageContent extends MessageContent {
    private String userId;
    private String name;
    private String displayName;
    private String portrait;

    public CardMessageContent() {
    }

    public CardMessageContent(String userId, String name, String displayName, String portrait) {
        this.userId = userId;
        this.name = name;
        this.displayName = displayName;
        this.portrait = portrait;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.content = userId;
        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("n", name);
            objWrite.put("d", displayName);
            objWrite.put("p", portrait);

            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        userId = payload.content;
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                name = jsonObject.optString("n");
                displayName = jsonObject.optString("d");
                portrait = jsonObject.optString("p");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[名片]:" + displayName;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.userId);
        dest.writeString(this.name != null ? this.name : "");
        dest.writeString(this.displayName != null ? this.displayName : "");
        dest.writeString(this.portrait != null ? this.portrait : "");
    }

    protected CardMessageContent(Parcel in) {
        super(in);
        this.userId = in.readString();
        this.name = in.readString();
        this.displayName = in.readString();
        this.portrait = in.readString();
    }

    public static final Creator<CardMessageContent> CREATOR = new Creator<CardMessageContent>() {
        @Override
        public CardMessageContent createFromParcel(Parcel source) {
            return new CardMessageContent(source);
        }

        @Override
        public CardMessageContent[] newArray(int size) {
            return new CardMessageContent[size];
        }
    };
}
