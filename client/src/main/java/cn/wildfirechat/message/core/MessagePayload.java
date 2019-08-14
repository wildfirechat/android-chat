package cn.wildfirechat.message.core;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.model.ProtoMessageContent;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

public class MessagePayload implements Parcelable {

    public /*MessageContentType*/ int contentType;
    public String searchableContent;
    public String pushContent;
    public String content;
    public byte[] binaryContent;

    public int mentionedType;
    public List<String> mentionedTargets;


    public MessageContentMediaType mediaType;
    public String remoteMediaUrl;


    //前面的属性都会在网络发送，下面的属性只在本地存储
    public String localMediaPath;

    //前面的属性都会在网络发送，下面的属性只在本地存储
    public String localContent;
    public String extra;

    public MessagePayload() {
    }

    public MessagePayload(ProtoMessageContent protoMessageContent) {
        this.contentType = protoMessageContent.getType();
        this.searchableContent = protoMessageContent.getSearchableContent();
        this.pushContent = protoMessageContent.getPushContent();
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
    }

    public ProtoMessageContent toProtoContent() {
        ProtoMessageContent out = new ProtoMessageContent();
        out.setType(contentType);
        out.setSearchableContent(searchableContent);
        out.setPushContent(pushContent);
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
        dest.writeInt(this.contentType);
        dest.writeString(this.searchableContent);
        dest.writeString(this.pushContent);
        dest.writeString(this.content);
        dest.writeByteArray(this.binaryContent);
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
        dest.writeInt(this.mediaType == null ? -1 : this.mediaType.ordinal());
        dest.writeString(this.remoteMediaUrl);
        dest.writeString(this.localMediaPath);
        dest.writeString(this.localContent);
    }

    protected MessagePayload(Parcel in) {
        this.contentType = in.readInt();
        this.searchableContent = in.readString();
        this.pushContent = in.readString();
        this.content = in.readString();
        this.binaryContent = in.createByteArray();
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
        int tmpMediaType = in.readInt();
        this.mediaType = tmpMediaType == -1 ? null : MessageContentMediaType.values()[tmpMediaType];
        this.remoteMediaUrl = in.readString();
        this.localMediaPath = in.readString();
        this.localContent = in.readString();
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
