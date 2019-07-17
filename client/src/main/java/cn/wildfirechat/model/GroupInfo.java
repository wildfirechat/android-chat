package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 17/12/2017.
 */


public class GroupInfo implements Parcelable {
    public enum GroupType {
        //member can add quit change group name and portrait, owner can do all the operations
        Normal(0),
        //every member can add quit change group name and portrait, no one can kickoff others
        Free(1),
        //member can only quit, owner can do all the operations
        Restricted(2);

        private int value;

        GroupType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static GroupType type(int type) {
            if (type >= 0 && type < GroupType.values().length) {
                return GroupType.values()[type];
            }

            throw new IllegalArgumentException("GroupType " + type + " is invalid");
        }
    }

    public String target;
    public String name;
    public String portrait;
    public String owner;
    public GroupType type;
    public int memberCount;
    public String extra;
    public long updateDt;


    //0 正常；1 全局禁言
    public int mute;

    //在group type为Restricted时，0 开放加入权限（群成员可以拉人，用户也可以主动加入）；1 只能群成员拉人入群；2 只能群管理拉人入群
    public int joinType;

    //是否运行群中普通成员私聊。0 运行，1不允许
    public int privateChat;

    //是否可以搜索到该群，功能暂未实现
    public int searchable;

    public GroupInfo() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.target);
        dest.writeString(this.name);
        dest.writeString(this.portrait);
        dest.writeString(this.owner);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeInt(this.memberCount);
        dest.writeString(this.extra);
        dest.writeLong(this.updateDt);
        dest.writeInt(this.mute);
        dest.writeInt(this.joinType);
        dest.writeInt(this.privateChat);
        dest.writeInt(this.searchable);
    }

    protected GroupInfo(Parcel in) {
        this.target = in.readString();
        this.name = in.readString();
        this.portrait = in.readString();
        this.owner = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : GroupType.values()[tmpType];
        this.memberCount = in.readInt();
        this.extra = in.readString();
        this.updateDt = in.readLong();
        this.mute = in.readInt();
        this.joinType = in.readInt();
        this.privateChat = in.readInt();
        this.searchable = in.readInt();
    }

    public static final Creator<GroupInfo> CREATOR = new Creator<GroupInfo>() {
        @Override
        public GroupInfo createFromParcel(Parcel source) {
            return new GroupInfo(source);
        }

        @Override
        public GroupInfo[] newArray(int size) {
            return new GroupInfo[size];
        }
    };
}
