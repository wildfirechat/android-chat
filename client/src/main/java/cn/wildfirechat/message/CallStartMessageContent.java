package cn.wildfirechat.message;

import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Start;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Call_Start, flag = PersistFlag.Persist)
public class CallStartMessageContent extends MessageContent {
    private String callId;
    private String targetId;
    private long connectTime;
    private long endTime;
    private boolean audioOnly;
    /*
     * 电话状态， 0 未接听，1 通话中，2 已结束。
     */
    private int status;

    public CallStartMessageContent() {
    }

    public CallStartMessageContent(String callId, String targetId, boolean audioOnly) {
        this.callId = callId;
        this.audioOnly = audioOnly;
        this.targetId = targetId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public long getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(long connectTime) {
        this.connectTime = connectTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public boolean isAudioOnly() {
        return audioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.content = callId;

        try {
            JSONObject objWrite = new JSONObject();
            if (connectTime > 0) {
                objWrite.put("c", connectTime);
            }

            if (endTime > 0) {
                objWrite.put("e", endTime);
            }

            if (status > 0) {
                objWrite.put("s", status);
            }

            objWrite.put("t", targetId);
            objWrite.put("a", audioOnly ? 1 : 0);

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
                connectTime = jsonObject.optLong("c", 0);
                endTime = jsonObject.optLong("e", 0);
                status = jsonObject.optInt("s", 0);
                targetId = jsonObject.optString("t");
                audioOnly = jsonObject.optInt("a") > 0;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[网络电话]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.callId);
        dest.writeLong(this.connectTime);
        dest.writeLong(this.endTime);
        dest.writeInt(this.status);
        dest.writeInt(audioOnly ? 1 : 0);
        dest.writeString(TextUtils.isEmpty(targetId) ? "" : targetId);
    }

    protected CallStartMessageContent(Parcel in) {
        this.callId = in.readString();
        this.connectTime = in.readLong();
        this.endTime = in.readLong();
        this.status = in.readInt();
        this.audioOnly = in.readInt() > 0;
        this.targetId = in.readString();
    }

    public static final Creator<CallStartMessageContent> CREATOR = new Creator<CallStartMessageContent>() {
        @Override
        public CallStartMessageContent createFromParcel(Parcel source) {
            return new CallStartMessageContent(source);
        }

        @Override
        public CallStartMessageContent[] newArray(int size) {
            return new CallStartMessageContent[size];
        }
    };
}
