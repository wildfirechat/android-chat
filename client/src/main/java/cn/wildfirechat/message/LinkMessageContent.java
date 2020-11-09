/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Link;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Link, flag = PersistFlag.Persist_And_Count)
public class LinkMessageContent extends MessageContent {
    private String title;
    private String contentDigest;
    private String url;
    private String thumbnailUrl;

    public LinkMessageContent() {
    }

    public LinkMessageContent(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public String getContentDigest() {
        return contentDigest;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();


        payload.searchableContent = title;


        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("d", contentDigest);
            objWrite.put("u", url);
            objWrite.put("t", thumbnailUrl);
            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        title = payload.searchableContent;


        try {
            if (payload.content != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                contentDigest = jsonObject.optString("c");
                url = jsonObject.optString("u");
                thumbnailUrl = jsonObject.optString("t");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return contentDigest;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.title);
        dest.writeString(this.contentDigest);
        dest.writeString(this.url);
        dest.writeString(this.thumbnailUrl);
    }

    protected LinkMessageContent(Parcel in) {
        super(in);
        this.title = in.readString();
        this.contentDigest = in.readString();
        this.url = in.readString();
        this.thumbnailUrl = in.readString();
    }

    public static final Creator<LinkMessageContent> CREATOR = new Creator<LinkMessageContent>() {
        @Override
        public LinkMessageContent createFromParcel(Parcel source) {
            return new LinkMessageContent(source);
        }

        @Override
        public LinkMessageContent[] newArray(int size) {
            return new LinkMessageContent[size];
        }
    };
}
