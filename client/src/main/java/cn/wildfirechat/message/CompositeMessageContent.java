/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.client.ClientService;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Composite_Message;

@ContentTag(type = ContentType_Composite_Message, flag = PersistFlag.Persist_And_Count)
public class CompositeMessageContent extends MessageContent {
    private String title;
    private List<Message> messages;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public CompositeMessageContent() {
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = this.title;
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            for (Message message : messages) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("uid", message.messageUid);
                msgObj.put("type", message.conversation.type.getValue());
                msgObj.put("target", message.conversation.target);
                msgObj.put("line", message.conversation.line);
                msgObj.put("from", message.sender);
                msgObj.put("tos", message.toUsers);
                msgObj.put("direction", message.direction.value());
                msgObj.put("status", message.status);
                msgObj.put("serverTime", message.serverTime);

                MessagePayload messagePayload = message.content.encode();
                messagePayload.contentType = message.content.getMessageContentType();

                msgObj.put("ctype", messagePayload.contentType);
                msgObj.put("csc", messagePayload.searchableContent);
                msgObj.put("cpc", messagePayload.pushContent);
                msgObj.put("cpd", messagePayload.pushData);
                msgObj.put("cc", messagePayload.content);
                if (messagePayload.binaryContent != null && messagePayload.binaryContent.length > 0) {
                    msgObj.put("cbc", Base64.encodeToString(messagePayload.binaryContent, Base64.DEFAULT));
                }
                msgObj.put("cmt", messagePayload.mentionedType);
                msgObj.put("cmts", new JSONArray(messagePayload.mentionedTargets));
                msgObj.put("ce", messagePayload.extra);

                if (message.content instanceof MediaMessageContent) {
                    msgObj.put("mt", ((MediaMessageContent) message.content).mentionedType);
                    msgObj.put("mru", ((MediaMessageContent) message.content).remoteUrl);
                }

                jsonArray.put(msgObj);
            }
            jsonObject.put("ms", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        payload.binaryContent = jsonObject.toString().getBytes();
        return payload;
    }

    // 只能从marsservice 进程调用
    public void decode(MessagePayload payload, ClientService service) {
        this.decode(payload, service::messageContentFromPayload);
    }

    // 只能从主进程调用
    public void decode(MessagePayload payload, ChatManager chatManager) {
        this.decode(payload, chatManager::messageContentFromPayload);
    }

    @Override
    public void decode(MessagePayload payload) {
        // CALL the other one
        throw new IllegalStateException("please call the alter one");
    }

    private void decode(MessagePayload payload, Converter contentConverter) {
        title = payload.content;
        try {
            List<Message> messages = new ArrayList<>();
            JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
            JSONArray jsonArray = jsonObject.getJSONArray("ms");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                Message message = new Message();
                message.messageUid = object.optLong("uid");
                message.conversation = new Conversation(Conversation.ConversationType.values()[object.optInt("type")], object.optString("target"), object.optInt("line"));
                message.sender = object.optString("from");
                message.toUsers = (String[]) object.opt("toUsers");
                message.direction = MessageDirection.values()[object.optInt("direction")];
                message.status = MessageStatus.status(object.optInt("status"));
                message.serverTime = object.optLong("serverTime");

                MessagePayload messagePayload = super.encode();
                messagePayload.contentType = object.optInt("ctype");
                messagePayload.searchableContent = object.optString("csc");
                messagePayload.pushContent = object.optString("cpc");
                messagePayload.pushData = object.optString("cpd");
                messagePayload.content = object.optString("cc");
                if (object.has("cbc")) {
                    messagePayload.binaryContent = Base64.decode(object.getString("cbc"), Base64.DEFAULT);
                }
                messagePayload.mentionedType = object.optInt("cmt");
                JSONArray cmts = object.optJSONArray("cmts");
                if (cmts != null && cmts.length() > 0) {
                    messagePayload.mentionedTargets = new ArrayList<>();
                    for (int j = 0; j < cmts.length(); j++) {
                        messagePayload.mentionedTargets.add(cmts.optString(j));
                    }
                }
                messagePayload.extra = object.optString("ce");
                messagePayload.mediaType = MessageContentMediaType.values()[object.optInt("mt")];
                messagePayload.remoteMediaUrl = object.optString("mru");

                message.content = contentConverter.convert(messagePayload, message.sender);
                messages.add(message);
            }
            this.messages = messages;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private interface Converter {
        MessageContent convert(MessagePayload payload, String sender);
    }

    @Override
    public String digest(Message message) {
        return "[聊天记录]: " + title;
    }

    public String compositeDigest() {
        List<Message> messages = getMessages();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.size() && i < 4; i++) {
            Message msg = messages.get(i);
            String sender = msg.sender;
            UserInfo userInfo = ChatManager.Instance().getUserInfo(sender, false);
            sb.append(userInfo.displayName + ": " + msg.content.digest(msg));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.title);
        dest.writeTypedList(this.messages);
    }

    protected CompositeMessageContent(Parcel in) {
        super(in);
        this.title = in.readString();
        this.messages = in.createTypedArrayList(Message.CREATOR);
    }

    public static final Creator<CompositeMessageContent> CREATOR = new Creator<CompositeMessageContent>() {
        @Override
        public CompositeMessageContent createFromParcel(Parcel source) {
            return new CompositeMessageContent(source);
        }

        @Override
        public CompositeMessageContent[] newArray(int size) {
            return new CompositeMessageContent[size];
        }
    };
}
