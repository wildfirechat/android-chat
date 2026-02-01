/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Location;

/**
 * 位置消息内容类
 * <p>
 * 用于表示地理位置类型的消息内容，支持位置信息的分享。
 * 包含位置标题、位置缩略图和经纬度坐标信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_Location, flag = PersistFlag.Persist_And_Count)
public class LocationMessageContent extends MessageContent {
    /**
     * 位置标题
     */
    private String title;
    /**
     * 位置缩略图，不跨进程传输
     */
    private Bitmap thumbnail;
    /**
     * 位置缩略图的字节数组
     */
    public byte[] thumbnailByte;
    /**
     * 位置坐标信息
     */
    private Location location;

    public LocationMessageContent() {
        location = new Location(LocationManager.GPS_PROVIDER);
    }

    public LocationMessageContent(String title, Bitmap thumbnail, Location location) {
        this.title = title;
        this.thumbnail = thumbnail;
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Bitmap getThumbnail() {
        if (thumbnail==null&&thumbnailByte!=null){
            thumbnail = BitmapFactory.decodeByteArray( this.thumbnailByte, 0,  this.thumbnailByte.length);
        }
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        this.thumbnailByte=baos.toByteArray();
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = title;
        payload.binaryContent = this.thumbnailByte;

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("lat", location.getLatitude());
            objWrite.put("long", location.getLongitude());

            payload.content = objWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        if (payload.binaryContent != null) {
            thumbnail = BitmapFactory.decodeByteArray(payload.binaryContent, 0, payload.binaryContent.length);
            this.thumbnailByte=payload.binaryContent;
        }
        title = payload.searchableContent;
        try {
            if (payload.content != null) {
                JSONObject jsonObject = new JSONObject(payload.content);
                location.setLatitude(jsonObject.optDouble("lat"));
                location.setLongitude(jsonObject.optDouble("long"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "位置";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.title);
        dest.writeByteArray(this.thumbnailByte);
        dest.writeParcelable(this.location, flags);
    }

    protected LocationMessageContent(Parcel in) {
        super(in);
        this.title = in.readString();
        this.thumbnailByte=in.createByteArray();
        this.location = in.readParcelable(Location.class.getClassLoader());
    }

    public static final Creator<LocationMessageContent> CREATOR = new Creator<LocationMessageContent>() {
        @Override
        public LocationMessageContent createFromParcel(Parcel source) {
            return new LocationMessageContent(source);
        }

        @Override
        public LocationMessageContent[] newArray(int size) {
            return new LocationMessageContent[size];
        }
    };
}
