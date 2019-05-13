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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_KICKOF_GROUP_MEMBER;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_KICKOF_GROUP_MEMBER, flag = PersistFlag.Persist)
public class KickoffGroupMemberNotificationContent extends GroupNotificationMessageContent {
    public String operator;
    public List<String> kickedMembers;

    public KickoffGroupMemberNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您把");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, operator));
            sb.append("把");
        }

        if (kickedMembers != null) {
            for (String member : kickedMembers) {
                sb.append(" ");
                sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, member));
            }
        }

        sb.append(" 移出了群组");
        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", operator);
            JSONArray objArray = new JSONArray();
            for (int i = 0; i < kickedMembers.size(); i++) {
                objArray.put(i, kickedMembers.get(i));
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
                JSONArray jsonArray = jsonObject.optJSONArray("ms");
                kickedMembers = new ArrayList<>();
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        kickedMembers.add(jsonArray.getString(i));
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
        dest.writeStringList(this.kickedMembers);
        dest.writeString(this.groupId);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
    }

    protected KickoffGroupMemberNotificationContent(Parcel in) {
        this.operator = in.readString();
        this.kickedMembers = in.createStringArrayList();
        this.groupId = in.readString();
        this.fromSelf = in.readByte() != 0;
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
    }

    public static final Creator<KickoffGroupMemberNotificationContent> CREATOR = new Creator<KickoffGroupMemberNotificationContent>() {
        @Override
        public KickoffGroupMemberNotificationContent createFromParcel(Parcel source) {
            return new KickoffGroupMemberNotificationContent(source);
        }

        @Override
        public KickoffGroupMemberNotificationContent[] newArray(int size) {
            return new KickoffGroupMemberNotificationContent[size];
        }
    };
}
