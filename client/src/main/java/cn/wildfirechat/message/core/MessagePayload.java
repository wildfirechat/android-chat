/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.core;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.model.ProtoMessageContent;

/**
 * 消息载荷类
 * <p>
 * 表示消息在网络传输和本地存储时的数据结构。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class MessagePayload implements Parcelable {

    /**
     * 消息类型
     */
    public /*MessageContentType*/ int type;

    /**
     * 可搜索内容
     */
    public String searchableContent;

    /**
     * 推送内容
     */
    public String pushContent;

    /**
     * 推送数据
     */
    public String pushData;

    /**
     * 消息内容
     */
    public String content;

    /**
     * 二进制内容
     */
    public byte[] binaryContent;

    /**
     * 扩展字段
     */
    public String extra;

    /**
     * @提醒类型
     */
    public int mentionedType;

    /**
     * @提醒对象列表
     */
    public List<String> mentionedTargets;

    /**
     * 媒体类型
     */
    public MessageContentMediaType mediaType;

    /**
     * 远程媒体URL
     */
    public String remoteMediaUrl;

    /**
     * 本地媒体路径（只在本地存储）
     */
    public String localMediaPath;

    /**
     * 本地内容
     */
    public String localContent;

    /**
     * 消息是否未加载
     */
    public int notLoaded;

    /**
     * 默认构造函数
     */
    public MessagePayload() {
    }

    /**
     * 从Proto消息内容构造
     *
     * @param protoMessageContent Proto消息内容
     */
    public MessagePayload(ProtoMessageContent protoMessageContent) {
        this.type = protoMessageContent.getType();
        this.searchableContent = protoMessageContent.getSearchableContent();
        this.pushContent = protoMessageContent.getPushContent();
        this.pushData = protoMessageContent.getPushData();
        this.content = protoMessageContent.getContent();
        this.binaryContent = protoMessageContent.getBinaryContent();
        this.localContent = protoMessageContent.getLocalContent();
        this.remoteMediaUrl = protoMessageContent.getRemoteMediaUrl();
        this.localMediaPath = protoMessageContent.getLocalMediaPath();
        this.mediaType = MessageContentMediaType.mediaType(protoMessageContent.getMediaType());
        this.mentionedType = protoMessageContent.getMentionedType();
        if (protoMessageContent.getMentionedTargets() != null) {
            this.mentionedTargets = Arrays.asList(protoMessageContent.getMentionedTargets());
        } else {
            this.mentionedTargets = new ArrayList<>();
        }
        this.extra = protoMessageContent.getExtra();
        this.notLoaded = protoMessageContent.getNotLoaded();
    }

    /**
     * 转换为Proto消息内容
     *
     * @return Proto消息内容
     */
    public ProtoMessageContent toProtoContent() {
        ProtoMessageContent out = new ProtoMessageContent();
        out.setType(type);
        out.setSearchableContent(searchableContent);
        out.setPushContent(pushContent);
        out.setPushData(pushData);
        out.setContent(content);
        out.setBinaryContent(binaryContent);
        out.setRemoteMediaUrl(remoteMediaUrl);
        out.setLocalContent(localContent);
        out.setLocalMediaPath(localMediaPath);
        out.setMediaType(mediaType != null ? mediaType.ordinal() : 0);
        out.setMentionedType(mentionedType);
        String[] targets;
        if (mentionedTargets != null && mentionedTargets.size() > 0) {
            targets = mentionedTargets.toArray(new String[0]);
        } else {
            targets = new String[0];
        }
        out.setMentionedTargets(targets);
        out.setExtra(extra);
        return out;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type);
        dest.writeString(this.searchableContent);
        dest.writeString(this.pushContent);
        dest.writeString(this.pushData);
        dest.writeString(this.content);
        dest.writeByteArray(this.binaryContent);
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
        dest.writeInt(this.mediaType == null ? -1 : this.mediaType.ordinal());
        dest.writeString(this.remoteMediaUrl);
        dest.writeString(this.localMediaPath);
        dest.writeString(this.localContent);
        dest.writeString(this.extra);
        dest.writeInt(this.notLoaded);
    }

    public MessagePayload(Parcel in) {
        this.type = in.readInt();
        this.searchableContent = in.readString();
        this.pushContent = in.readString();
        this.pushData = in.readString();
        this.content = in.readString();
        this.binaryContent = in.createByteArray();
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
        int tmpMediaType = in.readInt();
        this.mediaType = tmpMediaType == -1 ? null : MessageContentMediaType.values()[tmpMediaType];
        this.remoteMediaUrl = in.readString();
        this.localMediaPath = in.readString();
        this.localContent = in.readString();
        this.extra = in.readString();
        this.notLoaded = in.readInt();
    }

    public static final Creator<MessagePayload> CREATOR = new Creator<MessagePayload>() {
        @Override
        public MessagePayload createFromParcel(Parcel source) {
            return new MessagePayload(source);
        }

        @Override
        public MessagePayload[] newArray(int size) {
            return new MessagePayload[size];
        }
    };
}
