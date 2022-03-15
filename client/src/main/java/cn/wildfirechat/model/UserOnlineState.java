/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class UserOnlineState implements Parcelable {
    private String userId;
    private int customState;
    private String customText;
    private ClientState[] clientStates;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getCustomState() {
        return customState;
    }

    public void setCustomState(int customState) {
        this.customState = customState;
    }

    public String getCustomText() {
        return customText;
    }

    public void setCustomText(String customText) {
        this.customText = customText;
    }

    public ClientState[] getClientStates() {
        return clientStates;
    }

    public void setClientStates(ClientState[] clientStates) {
        this.clientStates = clientStates;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.userId);
        dest.writeInt(this.customState);
        dest.writeString(this.customText);
        dest.writeTypedArray(this.clientStates, flags);
    }

    public void readFromParcel(Parcel source) {
        this.userId = source.readString();
        this.customState = source.readInt();
        this.customText = source.readString();
        this.clientStates = source.createTypedArray(ClientState.CREATOR);
    }

    public UserOnlineState() {
    }

    protected UserOnlineState(Parcel in) {
        this.userId = in.readString();
        this.customState = in.readInt();
        this.customText = in.readString();
        this.clientStates = in.createTypedArray(ClientState.CREATOR);
    }

    public static final Parcelable.Creator<UserOnlineState> CREATOR = new Parcelable.Creator<UserOnlineState>() {
        @Override
        public UserOnlineState createFromParcel(Parcel source) {
            return new UserOnlineState(source);
        }

        @Override
        public UserOnlineState[] newArray(int size) {
            return new UserOnlineState[size];
        }
    };

    // 手机在线、web在线、pc在线
    public String desc() {
        if (this.customState > 0) {
            //0，未设置，1 忙碌，2 离开（主动设置），3 离开（长时间不操作），4 隐身，其它可以自主扩展。
            String[] cs = new String[]{"未设置", "忙碌", "离开(主动离开)", "离开(长时间未操作)", "隐身"};
            return this.customText + cs[this.customState];
        }

        String onlineClientDesc = "";
        String lastSeenDesc = "";
        for (ClientState s : clientStates) {
            //
            // /**
            //  Platform_UNSET = 0;
            //  Platform_iOS = 1;
            //  Platform_Android = 2;
            //  Platform_Windows = 3;
            //  Platform_OSX = 4;
            //  Platform_WEB = 5;
            //  Platform_WX = 6;
            //  Platform_LINUX = 7;
            //  Platform_iPad = 8;
            //  Platform_APad = 9;
            //  */
            // platform;
            //
            // //设备的在线状态，0是在线，1是有session但不在线，其它不在线。
            // state;
            //
            // //最后可见
            // lastSeen;

            String[] ps = new String[]{"未知", "iOS", "Android", "Windows", "mac", "Web", "小程序", "Linux", "iPad", "Android-Pad"};
            if (s.getState() == 0) {
                onlineClientDesc += ps[s.getPlatform()] + " ";
            } else {
                // TODO
                lastSeenDesc += ps[s.getPlatform()] + " ";
            }
        }

        if (!TextUtils.isEmpty(onlineClientDesc)) {
            return onlineClientDesc + "在线";
        } else if (!TextUtils.isEmpty(lastSeenDesc)) {
            return lastSeenDesc + "不久前在线";
        }
        //return "不在线";
        return "";
    }

}
