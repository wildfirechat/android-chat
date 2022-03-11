/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.utils.WeChatImageUtils;

/**
 * Created by heavyrain lee on 2017/12/6.
 *   图片展示高仿微信,并且
 *   占位图-缩略图-原图
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
        setImageSize();
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
        MessagePayload payload = super.encode();
        payload.searchableContent = "[图片]";
        //setImageSize();
        if (!TextUtils.isEmpty(localPath)) {
            try {
                int[] imageSize = WeChatImageUtils.getImageSizeByOrgSizeToWeChat((int)imageWidth,(int)imageHeight);
                Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(localPath), imageSize[0]/2, imageSize[1]/2);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                payload.binaryContent = baos.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if (thumbnailBytes != null){
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
     * 通过thumbnail 获取的宽高永远都是 200，不合适
     *
     * @param path
     */
    private void setImageSize(){
        if(TextUtils.isEmpty(localPath))
            return;
        int imageSize[] = WeChatImageUtils.getSize(new File(localPath));
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
