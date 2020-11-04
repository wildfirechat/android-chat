/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite;

import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
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


        switch (message.content.getMessageContentType()) {
            case MessageContentType.ContentType_Text:
                TextMessageContent textMessageContent = (TextMessageContent) message.content;
                item.title = textMessageContent.getContent();
                break;
            case MessageContentType.ContentType_Image:
                ImageMessageContent imageMessageContent = (ImageMessageContent) message.content;
                item.url = imageMessageContent.remoteUrl;
                if(imageMessageContent.getThumbnail() != null){
                    // TODO
                }
                break;
            case MessageContentType.ContentType_File:
                break;
            default:
                break;
        }

        return item;
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
