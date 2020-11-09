/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

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

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Call_Start, flag = PersistFlag.Persist)
public class CallStartMessageContent extends MessageContent {
    private String callId;
    // 多人视音频是有效
    private List<String> targetIds;
    private long connectTime;
    private long endTime;
    private boolean audioOnly;
    private String pin;


    /**
     * 0, UnKnown,
     * 1, Busy,
     * 2, SignalError,
     * 3, Hangup,
     * 4, MediaError,
     * 5, RemoteHangup,
     * 6, OpenCameraFailure,
     * 7, Timeout,
     * 8, AcceptByOtherClient
     */
    private int status;

    public CallStartMessageContent() {
    }

    public CallStartMessageContent(String callId, List<String> targetIds, boolean audioOnly) {
        this.callId = callId;
        this.audioOnly = audioOnly;
        this.targetIds = targetIds;
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

    public boolean isAudioOnly() {
        return audioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    public List<String> getTargetIds() {
        return targetIds;
    }

    public void setTargetIds(List<String> targetIds) {
        this.targetIds = targetIds;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
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

            objWrite.put("t", targetIds.get(0));
            JSONArray ts = new JSONArray(targetIds);
            objWrite.put("ts", ts);
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
                connectTime = jsonObject.optLong("c", 0);
                endTime = jsonObject.optLong("e", 0);
                status = jsonObject.optInt("s", 0);
                pin = jsonObject.optString("p");
                JSONArray array = jsonObject.optJSONArray("ts");
                targetIds = new ArrayList<>();
                if (array == null) {
                    targetIds.add(jsonObject.getString("t"));
                } else {
                    for (int i = 0; i < array.length(); i++) {
                        if (array.get(i) instanceof String) {
                            targetIds.add((String) array.get(i));
                        }
                    }
                }
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
        super.writeToParcel(dest, flags);
        dest.writeString(this.callId);
        dest.writeStringList(this.targetIds);
        dest.writeLong(this.connectTime);
        dest.writeLong(this.endTime);
        dest.writeByte(this.audioOnly ? (byte) 1 : (byte) 0);
        dest.writeInt(this.status);
        dest.writeString(this.pin);
    }

    protected CallStartMessageContent(Parcel in) {
        super(in);
        this.callId = in.readString();
        this.targetIds = new ArrayList<>();
        in.readStringList(this.targetIds);
        this.connectTime = in.readLong();
        this.endTime = in.readLong();
        this.audioOnly = in.readByte() != 0;
        this.status = in.readInt();
        this.pin = in.readString();
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
