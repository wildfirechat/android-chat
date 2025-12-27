/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 14/12/2017.
 */

public class JoinGroupRequest implements Parcelable {
    public String groupId;
    public String memberId;
    public String requestUserId;
    public String acceptUserId;
    public String reason;
    public String extra;
//    RequestStatus_Sent = 0,
//    RequestStatus_Accepted = 1,
//    RequestStatus_Rejected = 2
    public int status;
    public int readStatus;
    public long timestamp;

    public JoinGroupRequest() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.groupId);
        dest.writeString(this.memberId);
        dest.writeString(this.requestUserId);
        dest.writeString(this.acceptUserId);
        dest.writeString(this.reason);
        dest.writeString(this.extra);
        dest.writeInt(this.status);
        dest.writeInt(this.readStatus);
        dest.writeLong(this.timestamp);
    }

    protected JoinGroupRequest(Parcel in) {
        this.groupId = in.readString();
        this.memberId = in.readString();
        this.requestUserId = in.readString();
        this.acceptUserId = in.readString();
        this.reason = in.readString();
        this.extra = in.readString();
        this.status = in.readInt();
        this.readStatus = in.readInt();
        this.timestamp = in.readLong();
    }

    public static final Creator<JoinGroupRequest> CREATOR = new Creator<JoinGroupRequest>() {
        @Override
        public JoinGroupRequest createFromParcel(Parcel source) {
            return new JoinGroupRequest(source);
        }

        @Override
        public JoinGroupRequest[] newArray(int size) {
            return new JoinGroupRequest[size];
        }
    };
}
