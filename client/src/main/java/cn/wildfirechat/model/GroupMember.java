package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 17/12/2017.
 */


public class GroupMember implements Parcelable {
    public enum GroupMemberType {
        Normal(0),
        Manager(1),
        Owner(2),
        Silent(3),
        Removed(4);

        private int value;

        GroupMemberType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static GroupMemberType type(int type) {
            if (type >= 0 && type < GroupMemberType.values().length) {
                return GroupMemberType.values()[type];
            }

            throw new IllegalArgumentException("GroupMemberType " + type + " is invalid");
        }
    }

    public String groupId;
    public String memberId;
    public String alias;
    public GroupMemberType type;
    public long updateDt;


    public GroupMember() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.groupId);
        dest.writeString(this.memberId);
        dest.writeString(this.alias);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeLong(this.updateDt);
    }

    protected GroupMember(Parcel in) {
        this.groupId = in.readString();
        this.memberId = in.readString();
        this.alias = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : GroupMemberType.values()[tmpType];
        this.updateDt = in.readLong();
    }

    public static final Creator<GroupMember> CREATOR = new Creator<GroupMember>() {
        @Override
        public GroupMember createFromParcel(Parcel source) {
            return new GroupMember(source);
        }

        @Override
        public GroupMember[] newArray(int size) {
            return new GroupMember[size];
        }
    };
}
