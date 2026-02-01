/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.ChannelMenu;

/**
 * 频道菜单事件消息内容
 * <p>
 * 用于触发频道菜单项操作的透明消息。
 * 当用户点击频道菜单时，会发送此消息来通知其他用户。
 * </p>
 *
 * @author WildFireChat
 * @since 2022
 */
@ContentTag(type = MessageContentType.ContentType_Channel_Menu_Event, flag = PersistFlag.Transparent)
public class ChannelMenuEventMessageContent extends MessageContent {
    /**
     * 触发的菜单项
     */
    private ChannelMenu menu;

    public ChannelMenu getMenu() {
        return menu;
    }

    public void setMenu(ChannelMenu menu) {
        this.menu = menu;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = this.menu.toJsonObj().toString();
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
    }

    @Override
    public String digest(Message message) {
        return null;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(this.menu, flags);
    }

    public void readFromParcel(Parcel source) {
        this.menu = source.readParcelable(ChannelMenu.class.getClassLoader());
    }

    public ChannelMenuEventMessageContent() {
    }


    protected ChannelMenuEventMessageContent(Parcel in) {
        super(in);
        this.menu = in.readParcelable(ChannelMenu.class.getClassLoader());
    }

    public static final Creator<ChannelMenuEventMessageContent> CREATOR = new Creator<ChannelMenuEventMessageContent>() {
        @Override
        public ChannelMenuEventMessageContent createFromParcel(Parcel source) {
            return new ChannelMenuEventMessageContent(source);
        }

        @Override
        public ChannelMenuEventMessageContent[] newArray(int size) {
            return new ChannelMenuEventMessageContent[size];
        }
    };
}
