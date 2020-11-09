/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Card;


@ContentTag(type = ContentType_Card, flag = PersistFlag.Persist_And_Count)
public class CardMessageContent extends MessageContent {
    /**
     * 0，用户；1，群组；2，聊天室；3，频道
     */
    private int type;
    private String target;
    // 用户名，一般是type为用户时使用
    private String name;
    private String displayName;
    private String portrait;

    public CardMessageContent() {
    }

    public CardMessageContent(int type, String target, String displayName, String portrait) {
        this.type = type;
        this.target = target;
        this.displayName = displayName;
        this.portrait = portrait;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = target;
        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("t", type);
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
        target = payload.content;
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                type = jsonObject.optInt("t");
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
        if (type == 0)
            return "[个人名片]:" + displayName;
        else if (type == 1)
            return "[群组名片]:" + displayName;
        else if (type == 2)
            return "[聊天室名片]:" + displayName;
        else if (type == 3)
            return "[频道名片]:" + displayName;

        return "[名片]:" + displayName;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.type);
        dest.writeString(this.target);
        dest.writeString(this.name != null ? this.name : "");
        dest.writeString(this.displayName != null ? this.displayName : "");
        dest.writeString(this.portrait != null ? this.portrait : "");
    }

    protected CardMessageContent(Parcel in) {
        super(in);
        this.type = in.readInt();
        this.target = in.readString();
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
