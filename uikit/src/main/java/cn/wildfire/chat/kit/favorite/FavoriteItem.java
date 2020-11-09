/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.LinkMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

public class FavoriteItem {
    private int favId;
    private int favType;
    private long timestamp;
    private Conversation conversation;
    private String origin;
    private String sender;
    private String title;
    private String url;
    private String thumbUrl;
    private String data;

    public FavoriteItem() {

    }

    public FavoriteItem(int favId, int favType, long timestamp, Conversation conversation, String origin, String sender, String title, String url, String thumbUrl, String data) {
        this.favId = favId;
        this.favType = favType;
        this.timestamp = timestamp;
        this.conversation = conversation;
        this.origin = origin;
        this.sender = sender;
        this.title = title;
        this.url = url;
        this.thumbUrl = thumbUrl;
        this.data = data;
    }

    public static FavoriteItem fromMessage(Message message) {
        FavoriteItem item = new FavoriteItem();
        item.conversation = message.conversation;
        item.favType = message.content.getMessageContentType();
        item.sender = message.sender;
        switch (message.conversation.type) {
            case Group:
                GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(message.conversation.target, false);
                item.origin = groupInfo.name;
                break;
            case Single:
                item.origin = ChatManager.Instance().getUserDisplayName(message.sender);
                break;
            case Channel:
                ChannelInfo channelInfo = ChatManager.Instance().getChannelInfo(message.conversation.target, false);
                item.origin = channelInfo.name;
                break;
            case ChatRoom:

                break;
            default:
                break;
        }

        Map<String, Object> data = new HashMap<>();
        switch (message.content.getMessageContentType()) {
            case MessageContentType.ContentType_Text:
                TextMessageContent textMessageContent = (TextMessageContent) message.content;
                item.title = textMessageContent.getContent();
                break;
            case MessageContentType.ContentType_Image:
                ImageMessageContent imageMessageContent = (ImageMessageContent) message.content;
                item.url = imageMessageContent.remoteUrl;
                if (imageMessageContent.getThumbnail() != null) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    imageMessageContent.getThumbnail().compress(Bitmap.CompressFormat.PNG, 100, out);
                    String thumb = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
                    data.put("thumb", thumb);
                    item.data = new Gson().toJson(data);
                }
                break;
            case MessageContentType.ContentType_Video:
                VideoMessageContent videoMessageContent = (VideoMessageContent) message.content;
                item.url = videoMessageContent.remoteUrl;
                if (videoMessageContent.getThumbnail() != null) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    videoMessageContent.getThumbnail().compress(Bitmap.CompressFormat.PNG, 100, out);
                    String thumb = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
                    data.put("thumb", thumb);
                    //  TODO add duration to videoMessageContent
                    data.put("duration", 0);
                    item.data = new Gson().toJson(data);
                }
                break;
            case MessageContentType.ContentType_File:
                FileMessageContent fileMessageContent = (FileMessageContent) message.content;
                item.url = fileMessageContent.remoteUrl;
                item.title = fileMessageContent.getName();
                data.put("size", fileMessageContent.getSize());
                item.data = new Gson().toJson(data);
                break;

            case MessageContentType.ContentType_Composite_Message:
                CompositeMessageContent compositeMessageContent = (CompositeMessageContent) message.content;
                item.title = compositeMessageContent.getTitle();
                MessagePayload payload = compositeMessageContent.encode();
                item.data = Base64.encodeToString(payload.binaryContent, Base64.DEFAULT);
                break;
            case MessageContentType.ContentType_Voice:
                SoundMessageContent soundMessageContent = (SoundMessageContent) message.content;
                item.url = soundMessageContent.remoteUrl;
                data.put("duration", soundMessageContent.getDuration());
                item.data = new Gson().toJson(data);
                break;
            case MessageContentType.ContentType_Link:
                LinkMessageContent linkMessageContent = (LinkMessageContent) message.content;
                item.title = linkMessageContent.getTitle();
                item.thumbUrl = linkMessageContent.getThumbnailUrl();
                item.url = linkMessageContent.getUrl();
                break;
            default:
                break;
        }

        return item;
    }

    /**
     * 从收藏构建消息，本处返回返回的消息，和原始消息相比，数据不完整。
     *
     * @return
     */
    public Message toMessage() {
        Message message = new Message();
        message.conversation = conversation;
        message.sender = sender;
        switch (favType) {
            case MessageContentType.ContentType_Text:
                message.content = new TextMessageContent(title);
                break;
            case MessageContentType.ContentType_Image:
                ImageMessageContent imageMessageContent = new ImageMessageContent();
                message.content = imageMessageContent;
                imageMessageContent.remoteUrl = url;
                if (!TextUtils.isEmpty(data)) {
                    try {
                        JSONObject object = new JSONObject(data);
                        byte[] thumbnailBytes = Base64.decode(object.getString("thumb"), Base64.DEFAULT);
                        imageMessageContent.setThumbnailBytes(thumbnailBytes);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case MessageContentType.ContentType_Video:
                VideoMessageContent videoMessageContent = new VideoMessageContent();
                message.content = videoMessageContent;
                videoMessageContent.remoteUrl = url;
                if (!TextUtils.isEmpty(data)) {
                    try {
                        JSONObject object = new JSONObject(data);
                        byte[] thumbnailBytes = Base64.decode(object.getString("thumb"), Base64.DEFAULT);
                        videoMessageContent.setThumbnailBytes(thumbnailBytes);
                        // TODO duration
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case MessageContentType.ContentType_File:
                FileMessageContent fileMessageContent = new FileMessageContent();
                message.content = fileMessageContent;
                fileMessageContent.remoteUrl = url;
                fileMessageContent.setName(title);
                if (!TextUtils.isEmpty(data)) {
                    try {
                        JSONObject object = new JSONObject(data);
                        fileMessageContent.setSize(object.getInt("size"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                break;
            case MessageContentType.ContentType_Composite_Message:
                CompositeMessageContent compositeMessageContent = new CompositeMessageContent();
                message.content = compositeMessageContent;

                compositeMessageContent.setTitle(title);
                if (!TextUtils.isEmpty(data)) {
                    byte[] payloadBytes = Base64.decode(data, Base64.DEFAULT);
                    MessagePayload payload = new MessagePayload();
                    payload.content = title;
                    payload.binaryContent = payloadBytes;
                    compositeMessageContent.decode(payload, ChatManager.Instance());
                }

                break;
            case MessageContentType.ContentType_Voice:
                SoundMessageContent soundMessageContent = new SoundMessageContent();
                message.content = soundMessageContent;
                soundMessageContent.remoteUrl = url;
                if (!TextUtils.isEmpty(data)) {
                    try {
                        JSONObject object = new JSONObject(data);
                        soundMessageContent.setDuration(object.getInt("duration"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }

        return message;
    }

    public int getFavId() {
        return favId;
    }

    public void setFavId(int favId) {
        this.favId = favId;
    }

    public int getFavType() {
        return favType;
    }

    public void setFavType(int favType) {
        this.favType = favType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
