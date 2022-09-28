/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Composite_Message;

import android.os.Parcel;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

@ContentTag(type = ContentType_Composite_Message, flag = PersistFlag.Persist_And_Count)
public class CompositeMessageContent extends MediaMessageContent {
    private String title;
    //可能会非常大，不进行 ipc，需要使用时，在进行 decode
    private List<Message> messages;
    // 内部 ipc 使用
    private byte[] binaryContent;

    private boolean mediaCompositeMessageLoaded = false;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // 只允许在主进程调用
    public List<Message> getMessages() {
        if (isLoaded()) {
            if (!mediaCompositeMessageLoaded && !TextUtils.isEmpty(localPath) && new File(localPath).exists()) {
                decodeMediaCompositeMessages();
                mediaCompositeMessageLoaded = true;
            } else if (this.messages == null && this.binaryContent != null && this.binaryContent.length > 0) {
                decodeMessages(this.binaryContent, ChatManager.Instance()::messageContentFromPayload);
            }
        } else {
            if (this.binaryContent != null && this.binaryContent.length > 0) {
                decodeMessages(this.binaryContent, ChatManager.Instance()::messageContentFromPayload);
            }
        }
        return messages;
    }

    public boolean isLoaded() {
        if (TextUtils.isEmpty(remoteUrl)) {
            return true;
        } else {
            if (TextUtils.isEmpty(localPath)) {
                return false;
            }
            File file = new File(localPath);
            return file.exists();
        }
    }

    public void setMessages(List<Message> messages) {
        this.messages = new ArrayList<>();
        for (Message msg : messages) {
            if (msg.content instanceof ArticlesMessageContent) {
                List<LinkMessageContent> linkMessageContents = ((ArticlesMessageContent) msg.content).toLinkMessageContent();
                for (LinkMessageContent linkMessageContent : linkMessageContents) {
                    Message linkMsg = new Message(msg);
                    linkMsg.content = linkMessageContent;
                    this.messages.add(linkMsg);
                }

            } else {
                this.messages.add(msg);

            }
        }

        JSONObject object = this.encodeMessages(null);
        this.binaryContent = object.toString().getBytes();
    }

    public CompositeMessageContent() {
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = this.title;
        payload.binaryContent = this.binaryContent;
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
        super.decode(payload);
        title = payload.content;
        this.binaryContent = payload.binaryContent;
    }

    private JSONObject encodeMessages(MessagePayload targetPayload) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONArray binArray = null;
        long size = 0;
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
                if (!TextUtils.isEmpty(message.localExtra)) {
                    msgObj.put("le", message.localExtra);
                }

                MessagePayload messagePayload = message.content.encode();
                messagePayload.type = message.content.getMessageContentType();

                msgObj.put("ctype", messagePayload.type);
                if (!TextUtils.isEmpty(messagePayload.searchableContent)) {
                    msgObj.put("csc", messagePayload.searchableContent);
                    if (targetPayload != null) {
                        targetPayload.searchableContent = targetPayload.searchableContent + messagePayload.searchableContent + " ";
                    }
                }
                if (!TextUtils.isEmpty(messagePayload.pushContent))
                    msgObj.put("cpc", messagePayload.pushContent);
                if (!TextUtils.isEmpty(messagePayload.pushData))
                    msgObj.put("cpd", messagePayload.pushData);
                if (!TextUtils.isEmpty(messagePayload.content))
                    msgObj.put("cc", messagePayload.content);
                if (messagePayload.binaryContent != null && messagePayload.binaryContent.length > 0) {
                    msgObj.put("cbc", Base64.encodeToString(messagePayload.binaryContent, Base64.NO_WRAP));
                }
                msgObj.put("cmt", messagePayload.mentionedType);
                msgObj.put("cmts", new JSONArray(messagePayload.mentionedTargets));
                if (!TextUtils.isEmpty(messagePayload.extra))
                    msgObj.put("ce", messagePayload.extra);

                if (message.content instanceof MediaMessageContent) {
                    msgObj.put("mt", ((MediaMessageContent) message.content).mediaType.getValue());
                    if (!TextUtils.isEmpty(((MediaMessageContent) message.content).remoteUrl))
                        msgObj.put("mru", ((MediaMessageContent) message.content).remoteUrl);
                }

                if (binArray == null) {
                    size += msgObj.toString().getBytes().length;
                    if (size > 20480 && jsonArray.length() > 0) {
                        binArray = new JSONArray();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            binArray.put(i, jsonArray.get(i));
                        }
                    }
                }
                jsonArray.put(msgObj);
            }

            if (binArray != null && TextUtils.isEmpty(localPath)) {
                jsonObject.put("ms", jsonArray);

                File file = new File(ChatManager.Instance().getApplicationContext().getCacheDir(), "wcf-" + System.currentTimeMillis() + ".data");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(jsonObject.toString().getBytes());
                    localPath = file.getAbsolutePath();
                    mediaType = MessageContentMediaType.FILE;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // replace
                jsonObject.put("ms", binArray);
            } else {
                if (binArray != null) {
                    jsonObject.put("ms", binArray);
                } else {
                    jsonObject.put("ms", jsonArray);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (targetPayload != null) {
            targetPayload.binaryContent = jsonObject.toString().getBytes();
        }
        return jsonObject;
    }

    private void decodeMediaCompositeMessages() {
        File file = new File(localPath);
        if (!file.exists()) {
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytesArray = new byte[(int) file.length()];
            int len = fis.read(bytesArray);
            int offset = len;
            while (len > 0) {
                len = fis.read(bytesArray, offset, (int) (file.length() - offset));
                offset += len;
            }
            decodeMessages(bytesArray, ChatManager.Instance()::messageContentFromPayload);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void decodeMessages(byte[] binaryContent, Converter contentConverter) {
        try {
            List<Message> messages = new ArrayList<>();
            JSONObject jsonObject = new JSONObject(new String(binaryContent));
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
                message.localExtra = object.optString("le");

                MessagePayload messagePayload = super.encode();
                messagePayload.type = object.optInt("ctype");
                messagePayload.searchableContent = object.optString("csc");
                messagePayload.pushContent = object.optString("cpc");
                messagePayload.pushData = object.optString("cpd");
                messagePayload.content = object.optString("cc");
                if (object.has("cbc")) {
                    messagePayload.binaryContent = Base64.decode(object.getString("cbc"), Base64.NO_WRAP);
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
        for (int i = 0; i < messages.size() && i < 3; i++) {
            Message msg = messages.get(i);
            String sender = msg.sender;
            UserInfo userInfo = ChatManager.Instance().getUserInfo(sender, false);
            String digest = msg.content.digest(msg);
            if (digest.length() > 36) {
                digest = digest.substring(0, 33);
                digest += "...";
            }
            sb.append(userInfo.displayName + ": " + digest + "\n");
        }
        if (messages.size() > 3) {
            sb.append("...");
        } else {
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
        dest.writeByteArray(this.binaryContent);
    }

    public void readFromParcel(Parcel source) {
        this.title = source.readString();
        this.binaryContent = source.createByteArray();
    }

    protected CompositeMessageContent(Parcel in) {
        super(in);
        this.title = in.readString();
        this.binaryContent = in.createByteArray();
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
