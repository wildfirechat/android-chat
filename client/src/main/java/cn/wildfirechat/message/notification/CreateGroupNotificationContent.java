package cn.wildfirechat.message.notification;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_CREATE_GROUP;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_CREATE_GROUP, flag = PersistFlag.Persist)
public class CreateGroupNotificationContent extends GroupNotificationMessageContent {
    public String creator;
    public String groupName;

    public CreateGroupNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您创建了群组 ");
        } else {
            sb.append(ChatManager.Instance().getUserDisplayName(creator));
            sb.append("创建了群组 ");
        }
        sb.append(groupName);

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", creator);
            objWrite.put("n", groupName);
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
                creator = jsonObject.optString("o");
                groupName = jsonObject.optString("n");
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
        dest.writeString(this.creator);
        dest.writeString(this.groupName);
        dest.writeString(this.groupId);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
    }

    protected CreateGroupNotificationContent(Parcel in) {
        this.creator = in.readString();
        this.groupName = in.readString();
        this.groupId = in.readString();
        this.fromSelf = in.readByte() != 0;
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
    }

    public static final Creator<CreateGroupNotificationContent> CREATOR = new Creator<CreateGroupNotificationContent>() {
        @Override
        public CreateGroupNotificationContent createFromParcel(Parcel source) {
            return new CreateGroupNotificationContent(source);
        }

        @Override
        public CreateGroupNotificationContent[] newArray(int size) {
            return new CreateGroupNotificationContent[size];
        }
    };
}
