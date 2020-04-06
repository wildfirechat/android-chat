package cn.wildfirechat.message.notification;

import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Recall;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Recall, flag = PersistFlag.Persist)
public class RecallMessageContent extends NotificationMessageContent {
    private String operatorId;
    private long messageUid;

    private String originalSender;
    private int originalContentType;
    private String originalSearchableContent;
    private String originalContent;
    private String originalExtra;
    private long originalMessageTimestamp;

    public RecallMessageContent() {
    }

    public RecallMessageContent(String operatorId, long messageUid) {
        this.operatorId = operatorId;
        this.messageUid = messageUid;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.content = operatorId;
        payload.binaryContent = new StringBuffer().append(messageUid).toString().getBytes();
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        operatorId = payload.content;
        messageUid = Long.parseLong(new String(payload.binaryContent));

        if (!TextUtils.isEmpty(payload.extra)) {
            try {
                JSONObject obj = new JSONObject(payload.extra);
                this.originalSender = obj.optString("s");
                this.originalContentType = obj.optInt("t");
                this.originalSearchableContent = obj.optString("sc");
                this.originalContent = obj.optString("c");
                this.originalExtra = obj.optString("e");
                this.originalMessageTimestamp = obj.optLong("ts");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOriginalSender() {
        return originalSender;
    }

    public int getOriginalContentType() {
        return originalContentType;
    }

    public String getOriginalSearchableContent() {
        return originalSearchableContent;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public String getOriginalExtra() {
        return originalExtra;
    }

    public long getOriginalMessageTimestamp() {
        return originalMessageTimestamp;
    }

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    @Override
    public String formatNotification(Message message) {
        String notification = "%s撤回了一条消息";
        if (fromSelf) {
            notification = String.format(notification, "您");
        } else {
            String displayName;
            if (message.conversation.type == Conversation.ConversationType.Group) {
                displayName = ChatManager.Instance().getGroupMemberDisplayName(message.conversation.target, operatorId);
            } else {
                displayName = ChatManager.Instance().getUserDisplayName(operatorId);
            }
            notification = String.format(notification, displayName);
        }
        return notification;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.operatorId);
        dest.writeLong(this.messageUid);
        dest.writeString(this.originalSender);
        dest.writeInt(this.originalContentType);
        dest.writeString(this.originalSearchableContent);
        dest.writeString(this.originalContent);
        dest.writeString(this.originalExtra);
        dest.writeLong(this.originalMessageTimestamp);
    }

    protected RecallMessageContent(Parcel in) {
        super(in);
        this.operatorId = in.readString();
        this.messageUid = in.readLong();
        this.originalSender = in.readString();
        this.originalContentType = in.readInt();
        this.originalSearchableContent = in.readString();
        this.originalContent = in.readString();
        this.originalExtra = in.readString();
        this.originalMessageTimestamp = in.readLong();
    }

    public static final Creator<RecallMessageContent> CREATOR = new Creator<RecallMessageContent>() {
        @Override
        public RecallMessageContent createFromParcel(Parcel source) {
            return new RecallMessageContent(source);
        }

        @Override
        public RecallMessageContent[] newArray(int size) {
            return new RecallMessageContent[size];
        }
    };
}
