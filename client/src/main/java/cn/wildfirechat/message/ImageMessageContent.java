/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.util.ImageUtils;

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

    private static final String TAG = "ImageMessageContent";

    private Bitmap thumbnail; // 不跨进程传输
    private byte[] thumbnailBytes;

    private double imageWidth;
    private double imageHeight;
    @Deprecated
    private String thumbPara;

    public ImageMessageContent() {
        this.mediaType = MessageContentMediaType.IMAGE;
    }

    public ImageMessageContent(String path) {
        this.localPath = path;
        this.mediaType = MessageContentMediaType.IMAGE;
        Log.e(TAG,"ImageMessageContent");
        setImageSize(path);
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

    public void setThumbnailBytes(byte[] thumbnailBytes) {
        this.thumbnailBytes = thumbnailBytes;
    }

    @Override
    public MessagePayload encode() {
        Log.e(TAG,"encode");
        MessagePayload payload = super.encode();
        payload.searchableContent = "[图片]";

        if (!TextUtils.isEmpty(localPath)) {
            setImageSize(localPath);
            /*if (!TextUtils.isEmpty(thumbPara)) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(localPath, options);

                options.inJustDecodeBounds = true;
                imageWidth = bitmap.getWidth();
                imageHeight = bitmap.getHeight();
            } else {
                try {
                    Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(localPath), 200, 200);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                    payload.binaryContent = baos.toByteArray();
                    imageWidth = thumbnail.getWidth();
                    imageHeight = thumbnail.getHeight();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }*/
        } else {
            payload.binaryContent = thumbnailBytes;
        }

        if (imageHeight > 0 && imageWidth > 0) {
            try {
                JSONObject objWrite = new JSONObject();
                objWrite.put("w", imageWidth);
                objWrite.put("h", imageHeight);
                objWrite.put("tp", thumbPara);
                payload.content = objWrite.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        Log.e(TAG,"decode");
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

    /**
     * 获取本地图片的宽高
     * @param path
     */
    private void setImageSize(String path){
        int imageSize[] = ImageUtils.getSize(localPath);
        imageWidth = imageSize[0];
        imageHeight = imageSize[1];
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
