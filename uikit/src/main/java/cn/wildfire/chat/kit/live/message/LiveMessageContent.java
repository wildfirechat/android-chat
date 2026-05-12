/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Live_Streaming_Start;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 直播消息内容
 * <p>
 * 用于发送直播消息，包含直播的详细信息如主题、描述、时间等。
 * 支持音频直播、视频直播、观众模式等多种直播类型。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_Live_Streaming_Start, flag = PersistFlag.Persist_And_Count)
public class LiveMessageContent extends MessageContent {
    /**
     * 直播唯一标识
     */
    private String liveId;

    /**
     * 直播主持人ID
     */
    private String host;

    /**
     * 直播标题
     */
    private String title;

    /**
     * 直播描述
     */
    private String desc;

    /**
     * 直播开始时间
     */
    private long startTime;

    /**
     * 是否为纯音频直播
     */
    private boolean audioOnly;

    /**
     * 是否为观众模式，观众模式，禁止请求连麦
     */
    private boolean audience;


    public LiveMessageContent() {
    }

    public LiveMessageContent(String liveId, String host, String title, String desc, long startTime, boolean audioOnly, boolean audience) {
        this.liveId = liveId;
        this.host = host;
        this.title = title;
        this.desc = desc;
        this.startTime = startTime;
        this.audioOnly = audioOnly;
        this.audience = audience;
    }

    public String getLiveId() {
        return liveId;
    }

    public void setLiveId(String liveId) {
        this.liveId = liveId;
    }


    public boolean isAudioOnly() {
        return audioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public boolean isAudience() {
        return audience;
    }

    public void setAudience(boolean audience) {
        this.audience = audience;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = liveId;

        try {
            JSONObject objWrite = new JSONObject();

            if (host != null) {
                objWrite.put("h", host);
            }

            if (startTime > 0) {
                objWrite.put("s", startTime);
            }

            if (title != null) {
                objWrite.put("t", title);
            }

            if (desc != null) {
                objWrite.put("d", desc);
            }

            objWrite.put("audience", audience ? 1 : 0);

            objWrite.put("a", audioOnly ? 1 : 0);

            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        payload.pushContent = "直播";
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        liveId = payload.content;
        pushContent = payload.pushContent;

        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                host = jsonObject.optString("h");
                title = jsonObject.optString("t");
                desc = jsonObject.optString("d");
                startTime = jsonObject.optLong("s");
                audience = jsonObject.optInt("audience") > 0;
                audioOnly = jsonObject.optInt("a") > 0;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[直播]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(liveId);
        dest.writeString(host != null ? host : "");
        dest.writeString(title != null ? title : "");
        dest.writeString(desc != null ? desc : "");
        dest.writeLong(startTime);
        dest.writeByte(audioOnly ? (byte) 1 : (byte) 0);
        dest.writeByte(audience ? (byte) 1 : (byte) 0);
    }

    protected LiveMessageContent(Parcel in) {
        super(in);
        liveId = in.readString();
        host = in.readString();
        title = in.readString();
        desc = in.readString();

        startTime = in.readLong();
        audioOnly = in.readByte() != 0;
        audience = in.readByte() != 0;
    }

    public static final Creator<LiveMessageContent> CREATOR = new Creator<LiveMessageContent>() {
        @Override
        public LiveMessageContent createFromParcel(Parcel source) {
            return new LiveMessageContent(source);
        }

        @Override
        public LiveMessageContent[] newArray(int size) {
            return new LiveMessageContent[size];
        }
    };
}
