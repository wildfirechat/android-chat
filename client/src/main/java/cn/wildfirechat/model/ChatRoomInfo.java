package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatRoomInfo implements Parcelable {
    public static final int STATE_NORMAL = 0;
    public static final int STATE_NOT_START = 1;
    public static final int STATE_END = 2;

    public enum State {
        // 正常
        NORMAL(0),
        // 未开始
        NOT_START(1),
        // 已结束
        END(2);

        private int value;

        public int getValue() {
            return value;
        }

        State(int state) {
            this.value = state;
        }
    }

    public String chatRoomId;
    public String title;
    public String desc;
    public String portrait;
    public String extra;
    public State state;
    public int memberCount;
    public long createDt;
    public long updateDt;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.chatRoomId);
        dest.writeString(this.title);
        dest.writeString(this.desc);
        dest.writeString(this.portrait);
        dest.writeString(this.extra);
        dest.writeInt(this.state == null ? -1 : this.state.ordinal());
        dest.writeInt(this.memberCount);
        dest.writeLong(this.createDt);
        dest.writeLong(this.updateDt);
    }

    public ChatRoomInfo() {
    }

    protected ChatRoomInfo(Parcel in) {
        this.chatRoomId = in.readString();
        this.title = in.readString();
        this.desc = in.readString();
        this.portrait = in.readString();
        this.extra = in.readString();
        int tmpState = in.readInt();
        this.state = tmpState == -1 ? null : State.values()[tmpState];
        this.memberCount = in.readInt();
        this.createDt = in.readLong();
        this.updateDt = in.readLong();
    }

    public static final Creator<ChatRoomInfo> CREATOR = new Creator<ChatRoomInfo>() {
        @Override
        public ChatRoomInfo createFromParcel(Parcel source) {
            return new ChatRoomInfo(source);
        }

        @Override
        public ChatRoomInfo[] newArray(int size) {
            return new ChatRoomInfo[size];
        }
    };
}
