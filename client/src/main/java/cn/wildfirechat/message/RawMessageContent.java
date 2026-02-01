/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Text;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Unknown;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.QuoteInfo;

/**
 * 原始消息内容
 * <p>
 * 用于处理未解析的原始消息负载，通常用于内部消息处理或调试。
 * 此消息不会被持久化存储。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_Unknown, flag = PersistFlag.No_Persist)
public class RawMessageContent extends MessageContent {
    /**
     * 原始消息负载数据
     */
    public MessagePayload payload;


    public RawMessageContent() {
    }

    @Override
    public MessagePayload encode() {
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        this.payload = payload;
    }

    @Override
    public String digest(Message message) {
        return "";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        payload.writeToParcel(dest, flags);
    }

    protected RawMessageContent(Parcel in) {
        payload = new MessagePayload(in);
    }

    public static final Creator<RawMessageContent> CREATOR = new Creator<RawMessageContent>() {
        @Override
        public RawMessageContent createFromParcel(Parcel source) {
            return new RawMessageContent(source);
        }

        @Override
        public RawMessageContent[] newArray(int size) {
            return new RawMessageContent[size];
        }
    };
}
