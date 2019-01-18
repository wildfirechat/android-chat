package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heavyrain on 2017/12/13.
 */

public class GroupSearchResult implements Parcelable {
    public GroupInfo groupInfo;
    //0 march group name, 1 march group member name, 2 both
    public int marchedType;
    public List<String> marchedMembers;

    public GroupSearchResult() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.groupInfo, flags);
        dest.writeInt(this.marchedType);
        dest.writeList(marchedMembers != null ? marchedMembers : new ArrayList<String>());
    }

    protected GroupSearchResult(Parcel in) {
        this.groupInfo = in.readParcelable(GroupInfo.class.getClassLoader());
        this.marchedType = in.readInt();
        this.marchedMembers = in.readArrayList(ClassLoader.getSystemClassLoader());
    }

    public static final Creator<GroupSearchResult> CREATOR = new Creator<GroupSearchResult>() {
        @Override
        public GroupSearchResult createFromParcel(Parcel source) {
            return new GroupSearchResult(source);
        }

        @Override
        public GroupSearchResult[] newArray(int size) {
            return new GroupSearchResult[size];
        }
    };
}
