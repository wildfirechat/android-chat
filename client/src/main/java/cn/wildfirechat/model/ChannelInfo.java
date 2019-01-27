package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 17/12/2017.
 */


public class ChannelInfo implements Parcelable {
    public enum ChannelStatus {
        //member can add quit change group name and portrait, owner can do all the operations
        Public(0),
        //every member can add quit change group name and portrait, no one can kickoff others
        Private(1),
        //member can only quit, owner can do all the operations
        Destoryed(2);

        private int value;

        ChannelStatus(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static ChannelStatus status(int type) {
            if (type >= 0 && type < ChannelStatus.values().length) {
                return ChannelStatus.values()[type];
            }

            throw new IllegalArgumentException("GroupType " + type + " is invalid");
        }
    }

    public String channelId;
    public String name;
    public String portrait;
    public String desc;
    public String owner;
    public ChannelStatus status;
    public String extra;
    public long updateDt;

    public ChannelInfo() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.channelId);
        dest.writeString(this.name);
        dest.writeString(this.portrait);
        dest.writeString(this.owner);
        dest.writeString(this.desc);
        dest.writeInt(this.status == null ? -1 : this.status.ordinal());
        dest.writeString(this.extra);
        dest.writeLong(this.updateDt);
    }

    protected ChannelInfo(Parcel in) {
        this.channelId = in.readString();
        this.name = in.readString();
        this.portrait = in.readString();
        this.owner = in.readString();
        this.desc = in.readString();
        int tmpType = in.readInt();
        this.status = tmpType == -1 ? null : ChannelStatus.values()[tmpType];
        this.extra = in.readString();
        this.updateDt = in.readLong();
    }

    public static final Creator<ChannelInfo> CREATOR = new Creator<ChannelInfo>() {
        @Override
        public ChannelInfo createFromParcel(Parcel source) {
            return new ChannelInfo(source);
        }

        @Override
        public ChannelInfo[] newArray(int size) {
            return new ChannelInfo[size];
        }
    };
}
