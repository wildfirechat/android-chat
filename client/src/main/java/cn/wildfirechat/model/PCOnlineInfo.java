package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class PCOnlineInfo implements Parcelable {

    /**
     * PC在线类型
     * <p>
     * - PC_Online: PC客户端在线
     * - Web_Online: Web客户端在线
     * - WX_Online: WX小程序客户端在线
     */
    public enum PCOnlineType {
        PC_Online,
        Web_Online,
        WX_Online
    }

    private PCOnlineType type;
    private boolean isOnline;
    private String clientId;
    private String clientName;
    private long timestamp;

    public PCOnlineType getType() {
        return type;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static PCOnlineInfo infoFromStr(String value, PCOnlineType type) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String[] parts = value.split("\\|");
        if (parts.length >= 4) {
            PCOnlineInfo info = new PCOnlineInfo();
            info.type = type;
            info.timestamp = Long.parseLong(parts[0]);
            info.clientId = parts[2];
            info.clientName = parts[3];
            info.isOnline = true;
            return info;
        }

        return null;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeByte(this.isOnline ? (byte) 1 : (byte) 0);
        dest.writeString(this.clientId);
        dest.writeString(this.clientName);
        dest.writeLong(this.timestamp);
    }

    public PCOnlineInfo() {
    }

    protected PCOnlineInfo(Parcel in) {
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : PCOnlineType.values()[tmpType];
        this.isOnline = in.readByte() != 0;
        this.clientId = in.readString();
        this.clientName = in.readString();
        this.timestamp = in.readLong();
    }

    public static final Creator<PCOnlineInfo> CREATOR = new Creator<PCOnlineInfo>() {
        @Override
        public PCOnlineInfo createFromParcel(Parcel source) {
            return new PCOnlineInfo(source);
        }

        @Override
        public PCOnlineInfo[] newArray(int size) {
            return new PCOnlineInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PCOnlineInfo that = (PCOnlineInfo) o;

        if (isOnline != that.isOnline) return false;
        if (timestamp != that.timestamp) return false;
        if (type != that.type) return false;
        if (!clientId.equals(that.clientId)) return false;
        return clientName.equals(that.clientName);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (isOnline ? 1 : 0);
        result = 31 * result + clientId.hashCode();
        result = 31 * result + clientName.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
