/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message;

import android.graphics.BitmapFactory;
import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Sticker, flag = PersistFlag.Persist_And_Count)
public class StickerMessageContent extends MediaMessageContent {
    public int width;
    public int height;

    public StickerMessageContent() {
        this.mediaType = MessageContentMediaType.STICKER;
    }

    public StickerMessageContent(String localPath) {
        this.localPath = localPath;
        this.mediaType = MessageContentMediaType.STICKER;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(localPath, options);
        height = options.outHeight;
        width = options.outWidth;
    }


    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = "[动态表情]";

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("x", width);
            objWrite.put("y", height);
            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);

        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                width = jsonObject.optInt("x");
                height = jsonObject.optInt("y");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[动态表情]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
    }

    protected StickerMessageContent(Parcel in) {
        super(in);
        this.width = in.readInt();
        this.height = in.readInt();
    }

    public static final Creator<StickerMessageContent> CREATOR = new Creator<StickerMessageContent>() {
        @Override
        public StickerMessageContent createFromParcel(Parcel source) {
            return new StickerMessageContent(source);
        }

        @Override
        public StickerMessageContent[] newArray(int size) {
            return new StickerMessageContent[size];
        }
    };
}
