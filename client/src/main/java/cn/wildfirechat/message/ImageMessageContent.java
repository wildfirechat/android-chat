package cn.wildfirechat.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Image, flag = PersistFlag.Persist_And_Count)
public class ImageMessageContent extends MediaMessageContent {
    private Bitmap thumbnail; // 不跨进程传输
    private byte[] thumbnailBytes;

    private double imageWidth;
    private double imageHeight;
    private String thumbPara;

    public ImageMessageContent() {
    }

    public ImageMessageContent(String path) {
        this.localPath = path;
        mediaType = MessageContentMediaType.IMAGE;

    }

    public Bitmap getThumbnail() {
        if (thumbnail != null) {
            return thumbnail;
        }
        if (thumbnailBytes != null) {
            thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length);
        } else if (!TextUtils.isEmpty(localPath)) {
            thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(localPath), 200, 200);
        }
        return thumbnail;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = "[图片]";

        if (!TextUtils.isEmpty(localPath)) {
            if (!TextUtils.isEmpty(thumbPara)) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(localPath, options);

                options.inJustDecodeBounds = true;
                imageWidth = bitmap.getWidth();
                imageHeight = bitmap.getHeight();
                try {
                    JSONObject objWrite = new JSONObject();
                    objWrite.put("w", imageWidth);
                    objWrite.put("h", imageHeight);
                    objWrite.put("tp", thumbPara);
                    payload.content = objWrite.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                // TODO 缩略图
                Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(localPath), 200, 200);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                payload.binaryContent = baos.toByteArray();
            }
        } else {
            if (!TextUtils.isEmpty(thumbPara) && imageHeight > 0 && imageWidth > 0) {
                try {
                    JSONObject objWrite = new JSONObject();
                    objWrite.put("w", imageWidth);
                    objWrite.put("h", imageHeight);
                    objWrite.put("tp", thumbPara);
                    payload.content = objWrite.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                payload.binaryContent = thumbnailBytes;
            }
        }

        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        thumbnailBytes = payload.binaryContent;
        if (payload.content != null && !payload.content.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(payload.content);
                imageWidth = jsonObject.optDouble("w");
                imageHeight = jsonObject.optDouble("h");
                thumbPara = jsonObject.optString("tp");
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public String digest(Message message) {
        return "[图片]";
    }

    public double getImageWidth() {
        return imageWidth;
    }

    public double getImageHeight() {
        return imageHeight;
    }

    public String getThumbPara() {
        return thumbPara;
    }

    public void setThumbPara(String thumbPara) {
        this.thumbPara = thumbPara;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByteArray(this.thumbnailBytes);
        dest.writeDouble(this.imageWidth);
        dest.writeDouble(this.imageHeight);
        dest.writeString(this.thumbPara);
    }

    protected ImageMessageContent(Parcel in) {
        super(in);
        this.thumbnailBytes = in.createByteArray();
        this.imageWidth = in.readDouble();
        this.imageHeight = in.readDouble();
        this.thumbPara = in.readString();
    }

    public static final Creator<ImageMessageContent> CREATOR = new Creator<ImageMessageContent>() {
        @Override
        public ImageMessageContent createFromParcel(Parcel source) {
            return new ImageMessageContent(source);
        }

        @Override
        public ImageMessageContent[] newArray(int size) {
            return new ImageMessageContent[size];
        }
    };
}
