/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * 聊天室成员信息类
 * <p>
 * 用于表示聊天室的成员信息。
 * 包含成员数量和成员ID列表。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class ChatRoomMembersInfo implements Parcelable {
    /**
     * 成员数量
     */
    public int memberCount;

    /**
     * 成员ID列表
     */
    public List<String> members;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.memberCount);
        dest.writeStringList(this.members);
    }

    public ChatRoomMembersInfo() {
    }

    protected ChatRoomMembersInfo(Parcel in) {
        this.memberCount = in.readInt();
        this.members = in.createStringArrayList();
    }

    public static final Creator<ChatRoomMembersInfo> CREATOR = new Creator<ChatRoomMembersInfo>() {
        @Override
        public ChatRoomMembersInfo createFromParcel(Parcel source) {
            return new ChatRoomMembersInfo(source);
        }

        @Override
        public ChatRoomMembersInfo[] newArray(int size) {
            return new ChatRoomMembersInfo[size];
        }
    };
}
