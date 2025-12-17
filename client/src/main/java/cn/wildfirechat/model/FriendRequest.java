/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 14/12/2017.
 */

public class FriendRequest implements Parcelable {
    /**
     * RequestDirection_Send = 0,
     * RequestDirection_Receive = 1
     */
    public int direction;
    public String target;
    public String reason;
    public String extra;
    //    RequestStatus_Sent = 0,
//    RequestStatus_Accepted = 1,
//    RequestStatus_Rejected = 3
    public int status;
    public int readStatus;
    public long timestamp;

    public FriendRequest() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.direction);
        dest.writeString(this.target);
        dest.writeString(this.reason);
        dest.writeString(this.extra);
        dest.writeInt(this.status);
        dest.writeInt(this.readStatus);
        dest.writeLong(this.timestamp);
    }

    protected FriendRequest(Parcel in) {
        this.direction = in.readInt();
        this.target = in.readString();
        this.reason = in.readString();
        this.extra = in.readString();
        this.status = in.readInt();
        this.readStatus = in.readInt();
        this.timestamp = in.readLong();
    }

    public static final Creator<FriendRequest> CREATOR = new Creator<FriendRequest>() {
        @Override
        public FriendRequest createFromParcel(Parcel source) {
            return new FriendRequest(source);
        }

        @Override
        public FriendRequest[] newArray(int size) {
            return new FriendRequest[size];
        }
    };
}
