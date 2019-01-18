package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 17/12/2017.
 */

public class ModifyMyInfoEntry implements Parcelable {

    public ModifyMyInfoType type;
    public String value;

    public ModifyMyInfoEntry() {
    }

    public ModifyMyInfoEntry(ModifyMyInfoType type, String value) {
        this.type = type;
        this.value = value;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeString(this.value);
    }

    protected ModifyMyInfoEntry(Parcel in) {
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : ModifyMyInfoType.values()[tmpType];
        this.value = in.readString();
    }

    public static final Creator<ModifyMyInfoEntry> CREATOR = new Creator<ModifyMyInfoEntry>() {
        @Override
        public ModifyMyInfoEntry createFromParcel(Parcel source) {
            return new ModifyMyInfoEntry(source);
        }

        @Override
        public ModifyMyInfoEntry[] newArray(int size) {
            return new ModifyMyInfoEntry[size];
        }
    };
}
