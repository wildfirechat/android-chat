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
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Location, flag = PersistFlag.Persist_And_Count)
public class LocationMessageContent extends MessageContent {
    private String title;
    private Bitmap thumbnail;
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
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.searchableContent = title;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        payload.binaryContent = baos.toByteArray();

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
        if (payload.binaryContent != null) {
            thumbnail = BitmapFactory.decodeByteArray(payload.binaryContent, 0, payload.binaryContent.length);
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
        dest.writeParcelable(this.thumbnail, flags);
        dest.writeString(this.title != null ? this.title : "");
        dest.writeDouble(this.location.getLatitude());
        dest.writeDouble(this.location.getLongitude());
    }

    protected LocationMessageContent(Parcel in) {
        this.thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
        this.title = in.readString();
        this.location = new Location(LocationManager.GPS_PROVIDER);
        this.location.setLatitude(in.readDouble());
        this.location.setLongitude(in.readDouble());
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
