/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference.message;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

@ContentTag(flag = PersistFlag.Transparent, type = MessageContentType.ContentType_Conference_Command)
public class ConferenceCommandContent extends MessageContent {
    public interface ConferenceCommandType {
        //全体静音，只有主持人可以操作，结果写入conference profile中。带有参数是否允许成员自主解除静音。
        int MUTE_ALL_AUDIO = 0;
        //取消全体静音，只有主持人可以操作，结果写入conference profile中。带有参数是否邀请成员解除静音。
        int CANCEL_MUTE_ALL_AUDIO = 1;

        //要求某个用户更改静音状态，只有主持人可以操作。带有参数是否静音/解除静音。
        int REQUEST_MUTE_AUDIO = 2;
        //拒绝UNMUTE要求。（如果同意不需要通知对方同意)
        int REJECT_UNMUTE_REQUEST_AUDIO = 3;

        //普通用户申请解除静音，带有参数是请求，还是取消请求。
        int APPLY_UNMUTE_AUDIO = 4;
        //管理员批准解除静音申请，带有参数是同意，还是拒绝申请。
        int APPROVE_UNMUTE_AUDIO = 5;
        //管理员批准全部解除静音申请，带有参数是同意，还是拒绝申请。
        int APPROVE_ALL_UNMUTE_AUDIO = 6;

        //举手，带有参数是举手还是放下举手
        int HANDUP = 7;
        //主持人放下成员的举手
        int PUT_HAND_DOWN = 8;
        //主持人放下全体成员的举手
        int PUT_ALL_HAND_DOWN = 9;

        //录制，有参数是录制还是取消录制
        int RECORDING = 10;

        // 设置焦点用户
        int FOCUS = 11;
        // 取消设置焦点用户
        int CANCEL_FOCUS = 12;

        //全体静音，只有主持人可以操作，结果写入conference profile中。带有参数是否允许成员自主解除静音。
        int MUTE_ALL_VIDEO = 13;
        //取消全体静音，只有主持人可以操作，结果写入conference profile中。带有参数是否邀请成员解除静音。
        int CANCEL_MUTE_ALL_VIDEO = 14;

        //要求某个用户更改静音状态，只有主持人可以操作。带有参数是否静音/解除静音。
        int REQUEST_MUTE_VIDEO = 15;
        //拒绝UNMUTE要求。（如果同意不需要通知对方同意)
        int REJECT_UNMUTE_REQUEST_VIDEO = 16;

        //普通用户申请解除静音，带有参数是请求，还是取消请求。
        int APPLY_UNMUTE_VIDEO = 17;
        //管理员批准解除静音申请，带有参数是同意，还是拒绝申请。
        int APPROVE_UNMUTE_VIDEO = 18;
        //管理员批准全部解除静音申请，带有参数是同意，还是拒绝申请。
        int APPROVE_ALL_UNMUTE_VIDEO = 19;
    }

    private String conferenceId;
    private int commandType;
    private String targetUserId;
    private boolean boolValue;


    public ConferenceCommandContent() {
    }

    public ConferenceCommandContent(String conferenceId, int commandType) {
        super();
        this.conferenceId = conferenceId;
        this.commandType = commandType;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = this.conferenceId;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("t", this.commandType);
            if (this.boolValue) {
                jsonObject.put("b", true);
            }
            jsonObject.putOpt("u", this.targetUserId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        payload.binaryContent = jsonObject.toString().getBytes();
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        this.conferenceId = payload.content;
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                this.commandType = jsonObject.optInt("t");
                this.boolValue = jsonObject.optBoolean("b");
                this.targetUserId = jsonObject.optString("u");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String digest(Message message) {
        return null;
    }


    public String getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(String conferenceId) {
        this.conferenceId = conferenceId;
    }

    public int getCommandType() {
        return commandType;
    }

    public void setCommandType(int commandType) {
        this.commandType = commandType;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public boolean getBoolValue() {
        return boolValue;
    }

    public void setBoolValue(boolean boolValue) {
        this.boolValue = boolValue;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.conferenceId);
        dest.writeInt(this.commandType);
        dest.writeString(this.targetUserId);
        dest.writeByte(this.boolValue ? (byte) 1 : (byte) 0);
    }

    public void readFromParcel(Parcel source) {
        this.conferenceId = source.readString();
        this.commandType = source.readInt();
        this.targetUserId = source.readString();
        this.boolValue = source.readByte() != 0;
    }

    protected ConferenceCommandContent(Parcel in) {
        super(in);
        this.conferenceId = in.readString();
        this.commandType = in.readInt();
        this.targetUserId = in.readString();
        this.boolValue = in.readByte() != 0;
    }

    public static final Creator<ConferenceCommandContent> CREATOR = new Creator<ConferenceCommandContent>() {
        @Override
        public ConferenceCommandContent createFromParcel(Parcel source) {
            return new ConferenceCommandContent(source);
        }

        @Override
        public ConferenceCommandContent[] newArray(int size) {
            return new ConferenceCommandContent[size];
        }
    };
}
