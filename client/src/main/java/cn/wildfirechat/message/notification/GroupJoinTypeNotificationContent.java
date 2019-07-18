package cn.wildfirechat.message.notification;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.CONTENT_TYPE_CHANGE_JOINTYPE;

@ContentTag(type = CONTENT_TYPE_CHANGE_JOINTYPE, flag = PersistFlag.Persist)
public class GroupJoinTypeNotificationContent extends GroupNotificationMessageContent {
    public String operator;

    //在group type为Restricted时，0 开放加入权限（群成员可以拉人，用户也可以主动加入）；1 只能群成员拉人入群；2 只能群管理拉人入群
    public int type;

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, operator));
        }
        switch (type) {
            case 0:
                sb.append("开发了加入群组功能");
                break;
            case 1:
                sb.append("仅允许群成员邀请加入群组");
                break;
            case 2:
                sb.append("只关闭了加入群组功能");
                break;
            default:
                break;
        }

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", operator);
            objWrite.put("n", type + "");
            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                groupId = jsonObject.optString("g");
                operator = jsonObject.optString("o");
                type = Integer.parseInt(jsonObject.optString("n", "0"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.operator);
        dest.writeInt(this.type);
        dest.writeString(this.groupId);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
    }

    public GroupJoinTypeNotificationContent() {
    }

    protected GroupJoinTypeNotificationContent(Parcel in) {
        this.operator = in.readString();
        this.type = in.readInt();
        this.groupId = in.readString();
        this.fromSelf = in.readByte() != 0;
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
    }

    public static final Creator<GroupJoinTypeNotificationContent> CREATOR = new Creator<GroupJoinTypeNotificationContent>() {
        @Override
        public GroupJoinTypeNotificationContent createFromParcel(Parcel source) {
            return new GroupJoinTypeNotificationContent(source);
        }

        @Override
        public GroupJoinTypeNotificationContent[] newArray(int size) {
            return new GroupJoinTypeNotificationContent[size];
        }
    };
}
