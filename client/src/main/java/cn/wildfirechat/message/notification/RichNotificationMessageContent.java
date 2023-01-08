/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

@ContentTag(type = MessageContentType.ContentType_Rich_Notification, flag = PersistFlag.Persist_And_Count)
public class RichNotificationMessageContent extends NotificationMessageContent{
    // 富通知消息
    public String title;
    public String desc;
    public String remark;

    // @[@{@"key":@"登录账户", @"value":@"野火IM", @"color":@"#173155"}, @{@"key":@"登录地点", @"value":@"北京", @"color":@"#173155"}]
    public ArrayList<Data> datas;

    // 附加信息
    public String exName;
    public String exPortrait;
    public String exUrl;

    // 应用信息
    public String appId;

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.pushContent = this.title;
        payload.content = desc;

        JSONObject jObj = new JSONObject();
        try {
            jObj.put("remark", this.remark);
            jObj.put("exName", this.exName);
            jObj.put("exPortrait", this.exPortrait);
            jObj.put("exUrl", this.exUrl);
            jObj.put("appId", this.appId);
            jObj.put("datas", this.datas);
            payload.binaryContent = jObj.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        this.title = payload.pushContent;
        this.desc = payload.content;
        try {
            JSONObject jOjb = new JSONObject(new String(payload.binaryContent));
            this.remark = jOjb.optString("remark");
            this.exName = jOjb.optString("exName");
            this.exPortrait = jOjb.optString("exPortrait");
            this.exUrl = jOjb.optString("exUrl");
            this.appId = jOjb.optString("appId");
            JSONArray ds = jOjb.optJSONArray("datas");
            if (ds != null && ds.length() > 0) {
                this.datas = new ArrayList<>();
                for (int i = 0; i < ds.length(); i++) {
                    JSONObject jd = ds.getJSONObject(i);
                    Data d = new Data();
                    d.key = jd.getString("key");
                    d.value = jd.getString("value");
                    d.color = jd.getString("color");
                    this.datas.add(d);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String formatNotification(Message message) {
        return this.title;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.title);
        dest.writeString(this.desc);
        dest.writeString(this.remark);
        dest.writeList(this.datas);
        dest.writeString(this.exName);
        dest.writeString(this.exPortrait);
        dest.writeString(this.exUrl);
        dest.writeString(this.appId);
    }

    public void readFromParcel(Parcel source) {
        this.title = source.readString();
        this.desc = source.readString();
        this.remark = source.readString();
        this.datas = new ArrayList<Data>();
        source.readList(this.datas, Data.class.getClassLoader());
        this.exName = source.readString();
        this.exPortrait = source.readString();
        this.exUrl = source.readString();
        this.appId = source.readString();
    }

    public RichNotificationMessageContent() {
    }

    protected RichNotificationMessageContent(Parcel in) {
        super(in);
        this.title = in.readString();
        this.desc = in.readString();
        this.remark = in.readString();
        this.datas = new ArrayList<Data>();
        in.readList(this.datas, Data.class.getClassLoader());
        this.exName = in.readString();
        this.exPortrait = in.readString();
        this.exUrl = in.readString();
        this.appId = in.readString();
    }

    public static final Creator<RichNotificationMessageContent> CREATOR = new Creator<RichNotificationMessageContent>() {
        @Override
        public RichNotificationMessageContent createFromParcel(Parcel source) {
            return new RichNotificationMessageContent(source);
        }

        @Override
        public RichNotificationMessageContent[] newArray(int size) {
            return new RichNotificationMessageContent[size];
        }
    };

    public static class Data implements Parcelable {
        public String key;
        public String value;
        public String color;


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.key);
            dest.writeString(this.value);
            dest.writeString(this.color);
        }

        public void readFromParcel(Parcel source) {
            this.key = source.readString();
            this.value = source.readString();
            this.color = source.readString();
        }

        public Data() {
        }

        protected Data(Parcel in) {
            this.key = in.readString();
            this.value = in.readString();
            this.color = in.readString();
        }

        public static final Creator<Data> CREATOR = new Creator<Data>() {
            @Override
            public Data createFromParcel(Parcel source) {
                return new Data(source);
            }

            @Override
            public Data[] newArray(int size) {
                return new Data[size];
            }
        };
    }
}
