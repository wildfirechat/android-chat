package cn.wildfirechat.message;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Start;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Conference_Invite;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Conference_Invite, flag = PersistFlag.Persist_And_Count)
public class ConferenceInviteMessageContent extends MessageContent {
    private String callId;
    private String host;
    private String title;
    private String desc;

    private long startTime;
    private boolean audioOnly;
    private boolean audience;
    private String pin;


    public ConferenceInviteMessageContent() {
    }

    public ConferenceInviteMessageContent(String callId, String host, String title, String desc, long startTime, boolean audioOnly, boolean audience, String pin) {
        this.callId = callId;
        this.host = host;
        this.title = title;
        this.desc = desc;
        this.startTime = startTime;
        this.audioOnly = audioOnly;
        this.audience = audience;
        this.pin = pin;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }


    public boolean isAudioOnly() {
        return audioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
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
        MessagePayload payload = new MessagePayload();
        payload.content = callId;

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

            objWrite.put("audience", audience?1:0);

            objWrite.put("a", audioOnly ? 1 : 0);
            objWrite.put("p", pin);

            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        callId = payload.content;

        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                host = jsonObject.optString("h");
                title = jsonObject.optString("t");
                desc = jsonObject.optString("d");
                pin = jsonObject.optString("p");
                startTime = jsonObject.optLong("s");
                audience = jsonObject.optBoolean("audience");
                audioOnly = jsonObject.optInt("a") > 0;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[会议]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(callId);
        dest.writeString(host!=null ? host:"");
        dest.writeString(title!=null?title:"");
        dest.writeString(desc!=null?desc:"");
        dest.writeLong(startTime);
        dest.writeByte(audioOnly ? (byte) 1 : (byte) 0);
        dest.writeByte(audience ? (byte) 1 : (byte) 0);
        dest.writeString(pin!=null?pin:"");
    }

    protected ConferenceInviteMessageContent(Parcel in) {
        super(in);
        callId = in.readString();
        host = in.readString();
        title = in.readString();
        desc = in.readString();

        startTime = in.readLong();
        audioOnly = in.readByte() != 0;
        audience = in.readByte() != 0;
        pin = in.readString();
    }

    public static final Creator<ConferenceInviteMessageContent> CREATOR = new Creator<ConferenceInviteMessageContent>() {
        @Override
        public ConferenceInviteMessageContent createFromParcel(Parcel source) {
            return new ConferenceInviteMessageContent(source);
        }

        @Override
        public ConferenceInviteMessageContent[] newArray(int size) {
            return new ConferenceInviteMessageContent[size];
        }
    };
}
