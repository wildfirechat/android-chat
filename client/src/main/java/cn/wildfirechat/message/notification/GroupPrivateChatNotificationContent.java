package cn.wildfirechat.message.notification;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.CONTENT_TYPE_CHANGE_PRIVATECHAT;

@ContentTag(type = CONTENT_TYPE_CHANGE_PRIVATECHAT, flag = PersistFlag.Persist)
public class GroupPrivateChatNotificationContent extends GroupNotificationMessageContent {
    public String operator;

    //是否运行群中普通成员私聊。0 运行，1不允许
    public int type;

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, operator));
        }
        if (type == 0) {
            sb.append("开启了成员私聊");
        } else {
            sb.append("关闭了成员私聊");
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
        super.writeToParcel(dest, flags);
        dest.writeString(this.operator);
        dest.writeInt(this.type);
    }

    public GroupPrivateChatNotificationContent() {
    }

    protected GroupPrivateChatNotificationContent(Parcel in) {
        super(in);
        this.operator = in.readString();
        this.type = in.readInt();
    }

    public static final Creator<GroupPrivateChatNotificationContent> CREATOR = new Creator<GroupPrivateChatNotificationContent>() {
        @Override
        public GroupPrivateChatNotificationContent createFromParcel(Parcel source) {
            return new GroupPrivateChatNotificationContent(source);
        }

        @Override
        public GroupPrivateChatNotificationContent[] newArray(int size) {
            return new GroupPrivateChatNotificationContent[size];
        }
    };
}
