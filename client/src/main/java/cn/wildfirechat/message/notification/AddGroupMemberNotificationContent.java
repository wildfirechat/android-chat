package cn.wildfirechat.message.notification;

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
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_ADD_GROUP_MEMBER;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_ADD_GROUP_MEMBER, flag = PersistFlag.Persist)
public class AddGroupMemberNotificationContent extends NotificationMessageContent {
    public String invitor;
    public List<String> invitees;

    public AddGroupMemberNotificationContent() {
    }

    @Override
    public String formatNotification() {
        StringBuilder sb = new StringBuilder();
        UserInfo userInfo;
        if (fromSelf) {
            sb.append("您邀请");
        } else {
            userInfo = ChatManager.Instance().getUserInfo(invitor, false);
            sb.append(userInfo.displayName);
            sb.append("邀请");
        }

        if (invitees != null) {
            for (String member : invitees) {
                sb.append(" ");
                userInfo = ChatManager.Instance().getUserInfo(member, false);
                sb.append(userInfo.displayName);
            }
        }

        sb.append(" 加入了群组");
        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("o", invitor);
            JSONArray objArray = new JSONArray();
            for (int i = 0; i < invitees.size(); i++) {
                objArray.put(i, invitees.get(i));
            }
            objWrite.put("ms", objArray);

            payload.binaryContent = objWrite.toString().getBytes();
            return payload;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void decode(MessagePayload payload) {
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                invitor = jsonObject.optString("o");
                JSONArray jsonArray = jsonObject.optJSONArray("ms");
                invitees = new ArrayList<>();
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        invitees.add(jsonArray.getString(i));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest() {
        return formatNotification();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.invitor);
        dest.writeStringList(this.invitees);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
    }

    protected AddGroupMemberNotificationContent(Parcel in) {
        this.invitor = in.readString();
        this.invitees = in.createStringArrayList();
        this.fromSelf = in.readByte() != 0;
    }

    public static final Parcelable.Creator<AddGroupMemberNotificationContent> CREATOR = new Parcelable.Creator<AddGroupMemberNotificationContent>() {
        @Override
        public AddGroupMemberNotificationContent createFromParcel(Parcel source) {
            return new AddGroupMemberNotificationContent(source);
        }

        @Override
        public AddGroupMemberNotificationContent[] newArray(int size) {
            return new AddGroupMemberNotificationContent[size];
        }
    };
}
