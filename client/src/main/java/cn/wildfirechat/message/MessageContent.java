/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 消息内容基类
 * <p>
 * 所有消息内容类型必须继承此类，并实现编码解码逻辑。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public abstract class MessageContent implements Parcelable {
    /**
     * 从消息载荷解码消息内容
     *
     * @param payload 消息载荷
     */
    public void decode(MessagePayload payload) {
        this.extra = payload.extra;
        this.notLoaded = payload.notLoaded;
    }

    /**
     * 获取消息摘要文本
     *
     * @param message 消息对象
     * @return 消息摘要文本
     */
    public abstract String digest(Message message);

    /**
     * @提醒类型：0 普通消息, 1 部分提醒, 2 提醒全部
     */
    public int mentionedType;

    /**
     * 提醒对象，mentionedType为1时有效
     */
    public List<String> mentionedTargets;

    /**
     * 扩展字段，使用JSON格式，保留未来的可扩展性
     */
    public String extra;

    /**
     * 消息是否还没有从服务器同步下来
     */
    public int notLoaded;

    /**
     * 推送内容
     */
    public String pushContent;

    /**
     * 获取消息内容类型
     *
     * @return 消息内容类型
     */
    final public int getMessageContentType() {
        ContentTag tag = getClass().getAnnotation(ContentTag.class);
        if (tag != null) {
            return tag.type();
        }
        return -1;
    }

    /**
     * 获取消息持久化标志
     *
     * @return 消息持久化标志
     */
    final public PersistFlag getPersistFlag() {
        ContentTag tag = getClass().getAnnotation(ContentTag.class);
        if (tag != null) {
            return tag.flag();
        }
        return PersistFlag.No_Persist;
    }

    /**
     * 将消息内容编码为消息载荷
     *
     * @return 消息载荷
     */
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.type = getMessageContentType();
        payload.mentionedType = mentionedType;
        payload.mentionedTargets = mentionedTargets;
        payload.extra = extra;
        payload.pushContent = pushContent;
        return payload;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
        dest.writeString(this.extra);
        dest.writeString(this.pushContent);
        dest.writeInt(this.notLoaded);
    }

    /**
     * 默认构造函数
     */
    public MessageContent() {
    }

    protected MessageContent(Parcel in) {
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
        this.extra = in.readString();
        this.pushContent = in.readString();
        this.notLoaded = in.readInt();
    }
}
