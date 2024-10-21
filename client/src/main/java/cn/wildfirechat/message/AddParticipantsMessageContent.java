/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Add_Participant;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.remote.ChatManager;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Call_Add_Participant, flag = PersistFlag.Persist)
public class AddParticipantsMessageContent extends NotificationMessageContent {
    public static class ParticipantStatus implements Parcelable {
        public ParticipantStatus(String userId, long acceptTime, long joinTime, boolean videoMuted) {
            this.userId = userId;
            this.acceptTime = acceptTime;
            this.joinTime = joinTime;
            this.videoMuted = videoMuted;
        }

        public String userId;
        public long acceptTime;
        public long joinTime;
        public boolean videoMuted;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.userId);
            dest.writeLong(this.acceptTime);
            dest.writeLong(this.joinTime);
            dest.writeInt(videoMuted ? 1 : 0);
        }

        protected ParticipantStatus(Parcel in) {
            this.userId = in.readString();
            this.acceptTime = in.readLong();
            this.joinTime = in.readLong();
            this.videoMuted = in.readInt() > 0;
        }

        public static final Creator<ParticipantStatus> CREATOR = new Creator<ParticipantStatus>() {
            @Override
            public ParticipantStatus createFromParcel(Parcel source) {
                return new ParticipantStatus(source);
            }

            @Override
            public ParticipantStatus[] newArray(int size) {
                return new ParticipantStatus[size];
            }
        };
    }

    private String callId;
    private String initiator;
    private String pin;
    // autoAnswer 为 true 时，只允许包含一个用户
    private List<String> participants;
    private List<ParticipantStatus> existParticipants;
    private boolean audioOnly;
    // 设置为 true 时，新用户被邀请加入会议时，SDK会自动接听，然后加入通话
    private boolean autoAnswer;
    // 自动接听时，由那个端进行处理
    private String clientId;

    public AddParticipantsMessageContent() {
    }

    public AddParticipantsMessageContent(String callId, String initiator, List<String> participants, List<ParticipantStatus> existParticipants, boolean audioOnly, String pin) {
        this.callId = callId;
        this.initiator = initiator;
        this.pin = pin;
        this.audioOnly = audioOnly;
        this.participants = participants;
        this.existParticipants = existParticipants;
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

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public List<ParticipantStatus> getExistParticipants() {
        return existParticipants;
    }

    public void setExistParticipants(List<ParticipantStatus> existParticipants) {
        this.existParticipants = existParticipants;
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

    public boolean isAutoAnswer() {
        return autoAnswer;
    }

    public void setAutoAnswer(boolean autoAnswer) {
        this.autoAnswer = autoAnswer;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = callId;

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("initiator", initiator);
            JSONArray jsonArray = new JSONArray();
            for (String participant : participants) {
                jsonArray.put(participant);
            }
            objWrite.put("participants", jsonArray);
            objWrite.put("audioOnly", audioOnly ? 1 : 0);
            objWrite.put("pin", pin);

            JSONArray array = new JSONArray();
            List<String> epids = new ArrayList<>();
            for (ParticipantStatus status : existParticipants) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("userId", status.userId);
                jsonObject.put("acceptTime", status.acceptTime);
                jsonObject.put("joinTime", status.joinTime);
                jsonObject.put("videoMuted", status.videoMuted);
                array.put(jsonObject);
                epids.add(status.userId);
            }

            objWrite.put("existParticipants", array);
            objWrite.put("autoAnswer", this.autoAnswer);
            objWrite.put("clientId", this.clientId);
            payload.binaryContent = objWrite.toString().getBytes();

            JSONObject pushDataWrite = new JSONObject();
            pushDataWrite.put("callId", callId);
            pushDataWrite.put("audioOnly", audioOnly);
            if(participants != null && !participants.isEmpty()) {
                pushDataWrite.put("participants", participants);
            }

            if(!epids.isEmpty()) {
                pushDataWrite.put("existParticipants", epids);
            }


            payload.pushData = pushDataWrite.toString();
            payload.pushContent = "音视频通话邀请";
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        callId = payload.content;

        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                initiator = jsonObject.getString("initiator");
                JSONArray array = jsonObject.getJSONArray("participants");
                participants = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    participants.add(array.getString(i));
                }

                audioOnly = (jsonObject.optInt("audioOnly", 0) == 1);
                pin = jsonObject.optString("pin");

                array = jsonObject.getJSONArray("existParticipants");
                autoAnswer = jsonObject.optBoolean("autoAnswer");
                clientId = jsonObject.optString("clientId");
                existParticipants = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    ParticipantStatus status = new ParticipantStatus(object.getString("userId"), object.getLong("acceptTime"), object.getLong("joinTime"), object.optBoolean("videoMuted"));
                    existParticipants.add(status);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您邀请");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(message.conversation.target, initiator));
            sb.append("邀请");
        }

        if (participants != null) {
            for (String member : participants) {
                sb.append(" ");
                if (member.equals(ChatManager.Instance().getUserId())) {
                    sb.append("您");
                } else {
                    sb.append(ChatManager.Instance().getGroupMemberDisplayName(message.conversation.target, member));
                }
            }
        }

        sb.append(" 加入了通话");
        return sb.toString();
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
        dest.writeStringList(this.participants);
        dest.writeList(this.existParticipants);
        dest.writeByte(this.audioOnly ? (byte) 1 : (byte) 0);
        dest.writeByte(this.autoAnswer? (byte) 1 : (byte) 0);
        dest.writeString(pin);
        dest.writeString(this.clientId);
    }

    protected AddParticipantsMessageContent(Parcel in) {
        super(in);
        this.callId = in.readString();
        this.initiator = in.readString();
        this.participants = in.createStringArrayList();
        this.existParticipants = new ArrayList<ParticipantStatus>();
        in.readList(this.existParticipants, ParticipantStatus.class.getClassLoader());
        this.audioOnly = in.readByte() != 0;
        this.autoAnswer = in.readByte() != 0;
        this.pin = in.readString();
        this.clientId = in.readString();
    }

    public static final Creator<AddParticipantsMessageContent> CREATOR = new Creator<AddParticipantsMessageContent>() {
        @Override
        public AddParticipantsMessageContent createFromParcel(Parcel source) {
            return new AddParticipantsMessageContent(source);
        }

        @Override
        public AddParticipantsMessageContent[] newArray(int size) {
            return new AddParticipantsMessageContent[size];
        }
    };
}
