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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_ImageText;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_ImageText, flag = PersistFlag.Persist_And_Count)
public class ImageTextMessageContent extends MessageContent {
    private String title;
    private String content;
    private String url;
    private Bitmap thumbnail;

    public ImageTextMessageContent() {
    }

    public ImageTextMessageContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        payload.binaryContent = baos.toByteArray();

        payload.searchableContent = title;


        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("c", content);
            objWrite.put("u", url);
            payload.content = objWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        title = payload.searchableContent;
        if (payload.binaryContent != null) {
            thumbnail = BitmapFactory.decodeByteArray(payload.binaryContent, 0, payload.binaryContent.length);
        }

        try {
            if (payload.content != null) {
                JSONObject jsonObject = new JSONObject(payload.content);
                content = jsonObject.optString("c");
                url = jsonObject.optString("u");
            }
        } catch (JSONException e) {
            e.printStackTrace();
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
        dest.writeString(this.title);
        dest.writeString(this.content);
        dest.writeString(this.url);
        dest.writeParcelable(this.thumbnail, flags);
    }

    protected ImageTextMessageContent(Parcel in) {
        super(in);
        this.title = in.readString();
        this.content = in.readString();
        this.url = in.readString();
        this.thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Creator<ImageTextMessageContent> CREATOR = new Creator<ImageTextMessageContent>() {
        @Override
        public ImageTextMessageContent createFromParcel(Parcel source) {
            return new ImageTextMessageContent(source);
        }

        @Override
        public ImageTextMessageContent[] newArray(int size) {
            return new ImageTextMessageContent[size];
        }
    };
}
