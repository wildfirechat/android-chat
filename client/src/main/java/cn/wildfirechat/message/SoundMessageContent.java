package cn.wildfirechat.message;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Voice, flag = PersistFlag.Persist_And_Count)
public class SoundMessageContent extends MediaMessageContent {
    private int duration;


    public SoundMessageContent() {
    }

    public SoundMessageContent(String content) {

    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.searchableContent = "[语音]";
        payload.mediaType = MessageContentMediaType.VOICE;

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("duration", duration);
            payload.content = objWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        payload.remoteMediaUrl = remoteUrl;
        payload.localMediaPath = localPath;
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        try {
            if (payload.content != null) {
                JSONObject jsonObject = new JSONObject(payload.content);
                duration = jsonObject.optInt("duration");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        remoteUrl = payload.remoteMediaUrl;
        localPath = payload.localMediaPath;
    }

    @Override
    public String digest() {
        return "[语音]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.duration);
        dest.writeString(this.localPath);
        dest.writeString(this.remoteUrl);
    }

    protected SoundMessageContent(Parcel in) {
        this.duration = in.readInt();
        this.localPath = in.readString();
        this.remoteUrl = in.readString();
    }

    public static final Creator<SoundMessageContent> CREATOR = new Creator<SoundMessageContent>() {
        @Override
        public SoundMessageContent createFromParcel(Parcel source) {
            return new SoundMessageContent(source);
        }

        @Override
        public SoundMessageContent[] newArray(int size) {
            return new SoundMessageContent[size];
        }
    };
}
