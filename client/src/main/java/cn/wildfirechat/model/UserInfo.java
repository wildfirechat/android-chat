package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * Created by heavyrainlee on 14/12/2017.
 */

public class UserInfo implements Parcelable, Comparable<UserInfo> {
    public String uid;
    public String name;
    public String displayName;
    // 用户在群里面给自己设置的备注，不同群不一样
    public String groupAlias;
    // 我为好友设置的备注
    public String friendAlias;
    public String portrait;
    public int gender;
    public String mobile;
    public String email;
    public String address;
    public String company;
    public String social;
    public String extra;
    public long updateDt;
    //0 normal; 1 robot; 2 thing;
    public int type;

    public UserInfo() {
    }


    @Override
    public int compareTo(@NonNull UserInfo userInfo) {
        return displayName.compareTo(userInfo.displayName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uid);
        dest.writeString(this.name);
        dest.writeString(this.displayName);
        dest.writeString(this.groupAlias);
        dest.writeString(this.friendAlias);
        dest.writeString(this.portrait);
        dest.writeInt(this.gender);
        dest.writeString(this.mobile);
        dest.writeString(this.email);
        dest.writeString(this.address);
        dest.writeString(this.company);
        dest.writeString(this.social);
        dest.writeString(this.extra);
        dest.writeLong(this.updateDt);
        dest.writeInt(this.type);
    }

    protected UserInfo(Parcel in) {
        this.uid = in.readString();
        this.name = in.readString();
        this.displayName = in.readString();
        this.groupAlias = in.readString();
        this.friendAlias = in.readString();
        this.portrait = in.readString();
        this.gender = in.readInt();
        this.mobile = in.readString();
        this.email = in.readString();
        this.address = in.readString();
        this.company = in.readString();
        this.social = in.readString();
        this.extra = in.readString();
        this.updateDt = in.readLong();
        this.type = in.readInt();
    }

    public static final Creator<UserInfo> CREATOR = new Creator<UserInfo>() {
        @Override
        public UserInfo createFromParcel(Parcel source) {
            return new UserInfo(source);
        }

        @Override
        public UserInfo[] newArray(int size) {
            return new UserInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserInfo userInfo = (UserInfo) o;

        if (gender != userInfo.gender) return false;
        if (updateDt != userInfo.updateDt) return false;
        if (type != userInfo.type) return false;
        if (!uid.equals(userInfo.uid)) return false;
        if (name != null ? !name.equals(userInfo.name) : userInfo.name != null) return false;
        if (displayName != null ? !displayName.equals(userInfo.displayName) : userInfo.displayName != null)
            return false;
        if (groupAlias != null ? !groupAlias.equals(userInfo.groupAlias) : userInfo.groupAlias != null)
            return false;
        if (friendAlias != null ? !friendAlias.equals(userInfo.friendAlias) : userInfo.friendAlias != null)
            return false;
        if (portrait != null ? !portrait.equals(userInfo.portrait) : userInfo.portrait != null)
            return false;
        if (mobile != null ? !mobile.equals(userInfo.mobile) : userInfo.mobile != null)
            return false;
        if (email != null ? !email.equals(userInfo.email) : userInfo.email != null) return false;
        if (address != null ? !address.equals(userInfo.address) : userInfo.address != null)
            return false;
        if (company != null ? !company.equals(userInfo.company) : userInfo.company != null)
            return false;
        if (social != null ? !social.equals(userInfo.social) : userInfo.social != null)
            return false;
        return extra != null ? extra.equals(userInfo.extra) : userInfo.extra == null;
    }

    @Override
    public int hashCode() {
        int result = uid.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (groupAlias != null ? groupAlias.hashCode() : 0);
        result = 31 * result + (friendAlias != null ? friendAlias.hashCode() : 0);
        result = 31 * result + (portrait != null ? portrait.hashCode() : 0);
        result = 31 * result + gender;
        result = 31 * result + (mobile != null ? mobile.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (company != null ? company.hashCode() : 0);
        result = 31 * result + (social != null ? social.hashCode() : 0);
        result = 31 * result + (extra != null ? extra.hashCode() : 0);
        result = 31 * result + (int) (updateDt ^ (updateDt >>> 32));
        result = 31 * result + type;
        return result;
    }
}
