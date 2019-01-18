package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class ChatRoomMembersInfo implements Parcelable {
    public int memberCount;
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
