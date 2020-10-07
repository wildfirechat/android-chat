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
import cn.wildfirechat.model.QuoteInfo;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Text;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Text, flag = PersistFlag.Persist_And_Count)
public class TextMessageContent extends MessageContent {
    private String content;
    // 引用信息
    private QuoteInfo quoteInfo;

    public TextMessageContent() {
    }

    public TextMessageContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.searchableContent = content;
        payload.mentionedType = mentionedType;
        payload.mentionedTargets = mentionedTargets;
        if (quoteInfo != null) {
            JSONObject object = new JSONObject();
            try {
                object.put("quote", quoteInfo.encode());
                payload.binaryContent = object.toString().getBytes();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        content = payload.searchableContent;
        mentionedType = payload.mentionedType;
        mentionedTargets = payload.mentionedTargets;
        if (payload.binaryContent != null && payload.binaryContent.length > 0) {
            try {
                JSONObject object = new JSONObject(new String(payload.binaryContent));
                quoteInfo = new QuoteInfo();
                quoteInfo.decode(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String digest(Message message) {
        return content;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.content);
    }

    protected TextMessageContent(Parcel in) {
        super(in);
        this.content = in.readString();
    }

    public static final Creator<TextMessageContent> CREATOR = new Creator<TextMessageContent>() {
        @Override
        public TextMessageContent createFromParcel(Parcel source) {
            return new TextMessageContent(source);
        }

        @Override
        public TextMessageContent[] newArray(int size) {
            return new TextMessageContent[size];
        }
    };
}
