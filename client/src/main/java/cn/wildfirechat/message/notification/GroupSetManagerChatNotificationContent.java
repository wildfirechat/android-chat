package cn.wildfirechat.message.notification;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.CONTENT_TYPE_SET_MANAGER;

@ContentTag(type = CONTENT_TYPE_SET_MANAGER, flag = PersistFlag.Persist)
public class GroupSetManagerChatNotificationContent extends GroupNotificationMessageContent {
    public String operator;
    // 1, 设置为管理员；0，取消管理员
    public int type;

    public List<String> memberIds;

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, operator));
        }
        sb.append("把");
        if (memberIds != null) {
            for (String member : memberIds) {
                sb.append(" ");
                sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, member));
            }
            sb.append(" ");
        }
        if (type == 0) {
            sb.append("取消了管理员");
        } else {
            sb.append("设置为了管理员");
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
            JSONArray objArray = new JSONArray();
            for (String id : memberIds) {
                objArray.put(id);
            }
            objWrite.put("ms", objArray);
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
                JSONArray jsonArray = jsonObject.getJSONArray("ms");
                memberIds = new ArrayList<>();
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        memberIds.add(jsonArray.getString(i));
                    }
                }
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
        dest.writeStringList(this.memberIds);
        dest.writeString(this.groupId);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
    }

    public GroupSetManagerChatNotificationContent() {
    }

    protected GroupSetManagerChatNotificationContent(Parcel in) {
        this.operator = in.readString();
        this.type = in.readInt();
        this.memberIds = in.createStringArrayList();
        this.groupId = in.readString();
        this.fromSelf = in.readByte() != 0;
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
    }

    public static final Creator<GroupSetManagerChatNotificationContent> CREATOR = new Creator<GroupSetManagerChatNotificationContent>() {
        @Override
        public GroupSetManagerChatNotificationContent createFromParcel(Parcel source) {
            return new GroupSetManagerChatNotificationContent(source);
        }

        @Override
        public GroupSetManagerChatNotificationContent[] newArray(int size) {
            return new GroupSetManagerChatNotificationContent[size];
        }
    };
}
