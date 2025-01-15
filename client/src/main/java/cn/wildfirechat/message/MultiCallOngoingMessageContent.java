/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

@ContentTag(type = MessageContentType.ContentType_Call_Multi_Call_Ongoing, flag = PersistFlag.Transparent)
public class MultiCallOngoingMessageContent extends MessageContent {
    private String callId;
    private String initiator;
    private boolean audioOnly;
    private List<String> targets;

    public MultiCallOngoingMessageContent() {
    }

    public MultiCallOngoingMessageContent(String callId, String initiator, boolean audioOnly, List<String> targets) {
        this.callId = callId;
        this.initiator = initiator;
        this.audioOnly = audioOnly;
        this.targets = targets;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = callId;
        JSONObject object = new JSONObject();
        try {
            object.put("initiator", this.initiator);
            JSONArray arr = new JSONArray();
            for (int i = 0; i < targets.size(); i++) {
                arr.put(i, targets.get(i));
            }
            object.put("targets", arr);
            object.put("audioOnly", this.audioOnly ? 1 : 0);
            payload.binaryContent = object.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        this.callId = payload.content;
        try {
            JSONObject object = new JSONObject(new String(payload.binaryContent));
            this.initiator = object.optString("initiator");
            this.targets = new ArrayList<>();
            JSONArray array = object.optJSONArray("targets");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    targets.add(array.optString(i));
                }
            }
            this.audioOnly = object.optInt("audioOnly") == 1;
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String digest(Message message) {
        return null;
    }


    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public boolean isAudioOnly() {
        return audioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.callId);
        dest.writeString(this.initiator);
        dest.writeByte(this.audioOnly ? (byte) 1 : (byte) 0);
        dest.writeStringList(this.targets);
    }

    public void readFromParcel(Parcel source) {
        this.callId = source.readString();
        this.initiator = source.readString();
        this.audioOnly = source.readByte() != 0;
        this.targets = source.createStringArrayList();
    }

    protected MultiCallOngoingMessageContent(Parcel in) {
        super(in);
        this.callId = in.readString();
        this.initiator = in.readString();
        this.audioOnly = in.readByte() != 0;
        this.targets = in.createStringArrayList();
    }

    public static final Creator<MultiCallOngoingMessageContent> CREATOR = new Creator<MultiCallOngoingMessageContent>() {
        @Override
        public MultiCallOngoingMessageContent createFromParcel(Parcel source) {
            return new MultiCallOngoingMessageContent(source);
        }

        @Override
        public MultiCallOngoingMessageContent[] newArray(int size) {
            return new MultiCallOngoingMessageContent[size];
        }
    };
}
