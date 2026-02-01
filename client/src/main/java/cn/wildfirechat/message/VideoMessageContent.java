/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Parcel;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;


import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.VideoParam;
import cn.wildfirechat.utils.WeChatImageUtils;

/**
 * 视频消息内容类
 * <p>
 * 用于表示视频类型的消息内容，支持小视频的发送和接收。
 * 包含视频时长、缩略图等信息，便于在会话列表中预览。
 * 视频时长单位为毫秒。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = MessageContentType.ContentType_Video, flag = PersistFlag.Persist_And_Count)
public class VideoMessageContent extends MediaMessageContent {

    private static final String TAG = "VideoMessageContent";
    /**
     * 视频缩略图
     */
    private Bitmap thumbnail;
    /**
     * 视频缩略图的字节数组
     */
    private byte[] thumbnailBytes;

    /**
     * 视频时长，单位为毫秒
     */
    private long duration ;

    // 所有消息都需要一个默认构造函数
    public VideoMessageContent() {
        this.mediaType = MessageContentMediaType.VIDEO;
    }

    public VideoMessageContent(String videoPath) {
        this.localPath = videoPath;
        this.mediaType = MessageContentMediaType.VIDEO;
        if (!TextUtils.isEmpty(localPath)) {
            VideoParam videoParam = WeChatImageUtils.getVideoParam(localPath);
            duration = videoParam.getDuration();
            thumbnailBytes = videoParam.getThumbnailBytes();
        }
    }

    public Bitmap getThumbnail() {
        if (thumbnail != null) {
            return thumbnail;
        }
        if (thumbnailBytes != null) {
            thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length);
        } else {
            if (!TextUtils.isEmpty(localPath)) {
                thumbnail = ThumbnailUtils.createVideoThumbnail(localPath, MediaStore.Video.Thumbnails.MICRO_KIND);
                thumbnail = ThumbnailUtils.extractThumbnail(thumbnail, 320, 240,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            }
        }
        return thumbnail;
    }

    public void setThumbnailBytes(byte[] thumbnailBytes) {
        this.thumbnailBytes = thumbnailBytes;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = "[视频]";
        if ((thumbnailBytes == null || thumbnailBytes.length == 0 || duration == 0) && !TextUtils.isEmpty(localPath)) {
            try {
                VideoParam videoParam = WeChatImageUtils.getVideoParam(localPath);
                duration = videoParam.getDuration();
                thumbnailBytes = videoParam.getThumbnailBytes();
                payload.binaryContent = thumbnailBytes;
                Log.e(TAG,"binaryContent="+ payload.binaryContent.length/1024+"kb");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            payload.binaryContent = thumbnailBytes;
        }
        Log.e(TAG, "videoMessageContent encode " + payload.binaryContent.length + " " + duration);
        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("d", duration);
            objWrite.put("duration", duration);
            payload.content = objWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        thumbnailBytes = payload.binaryContent;
        try {
            JSONObject jsonObject = new JSONObject(payload.content);
            if(jsonObject.has("d")) {
                duration = jsonObject.optLong("d");
            } else {
                duration = jsonObject.optLong("duration");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,e.getMessage());
        }
    }

    @Override
    public String digest(Message message) {
        return "[视频]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByteArray(this.thumbnailBytes);
        dest.writeLong(this.duration);
    }

    public long getDuration() {
        return duration;
    }

    protected VideoMessageContent(Parcel in) {
        super(in);
        this.thumbnailBytes = in.createByteArray();
        this.duration = in.readLong();
    }

    public static final Creator<VideoMessageContent> CREATOR = new Creator<VideoMessageContent>() {
        @Override
        public VideoMessageContent createFromParcel(Parcel source) {
            return new VideoMessageContent(source);
        }

        @Override
        public VideoMessageContent[] newArray(int size) {
            return new VideoMessageContent[size];
        }
    };



}
