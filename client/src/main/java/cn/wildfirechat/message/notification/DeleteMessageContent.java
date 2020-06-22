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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Delete;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Recall;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Delete, flag = PersistFlag.No_Persist)
public class DeleteMessageContent extends NotificationMessageContent {
    private String operatorId;
    private long messageUid;

    public DeleteMessageContent() {
    }

    public DeleteMessageContent(String operatorId, long messageUid) {
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
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    @Override
    public String formatNotification(Message message) {
        String notification = "消息已删除";
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
    }

    protected DeleteMessageContent(Parcel in) {
        super(in);
        this.operatorId = in.readString();
        this.messageUid = in.readLong();
    }

    public static final Creator<DeleteMessageContent> CREATOR = new Creator<DeleteMessageContent>() {
        @Override
        public DeleteMessageContent createFromParcel(Parcel source) {
            return new DeleteMessageContent(source);
        }

        @Override
        public DeleteMessageContent[] newArray(int size) {
            return new DeleteMessageContent[size];
        }
    };
}
