/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ClientState implements Parcelable {
    private int platform;
    private int state;
    private long lastSeen;

    public ClientState(int platform, int state, long lastSeen) {
        this.platform = platform;
        this.state = state;
        this.lastSeen = lastSeen;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.platform);
        dest.writeInt(this.state);
        dest.writeLong(this.lastSeen);
    }

    public void readFromParcel(Parcel source) {
        this.platform = source.readInt();
        this.state = source.readInt();
        this.lastSeen = source.readLong();
    }

    protected ClientState(Parcel in) {
        this.platform = in.readInt();
        this.state = in.readInt();
        this.lastSeen = in.readLong();
    }

    public static final Creator<ClientState> CREATOR = new Creator<ClientState>() {
        @Override
        public ClientState createFromParcel(Parcel source) {
            return new ClientState(source);
        }

        @Override
        public ClientState[] newArray(int size) {
            return new ClientState[size];
        }
    };
}
