/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.client;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 添加好友来源
 */
public class FriendSource implements Parcelable {
    /**
     * 未知
     */
    public static final int Type_Unknown = 0;
    /**
     * 搜索
     */
    public static final int Type_Search = 1;
    /**
     * 群组，targetId 为群主 Id
     */
    public static final int Type_Group = 2;
    /**
     * 二维码，targetId 为分享二维码的用户 id
     */
    public static final int Type_QRCode = 3;
    /**
     * 用户名片，targetId 为分享名片的用户 id
     */
    public static final int Type_Card = 4;

    public int type = Type_Unknown;
    public String targetId;

    public FriendSource(int type, String targetId) {
        this.type = type;
        this.targetId = targetId;
    }

    public static String buildFriendSourceExtra(int sourceType, String sourceTargetId) {
        JSONObject obj = new JSONObject();
        JSONObject source = new JSONObject();
        try {
            source.put("t", sourceType);
            if (!TextUtils.isEmpty(sourceTargetId)) {
                source.put("i", sourceTargetId);
            }
            obj.put("s", sourceTargetId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return obj.toString();
    }

    public static FriendSource getFriendSource(String memberExtra) {
        try {
            JSONObject obj = new JSONObject(memberExtra);
            JSONObject s = obj.optJSONObject("s");
            if (s != null) {
                int type = s.optInt("t");
                String target = s.optString("i");
                return new FriendSource(type, target);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return new FriendSource(Type_Unknown, null);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type);
        dest.writeString(this.targetId);
    }

    public void readFromParcel(Parcel source) {
        this.type = source.readInt();
        this.targetId = source.readString();
    }

    protected FriendSource(Parcel in) {
        this.type = in.readInt();
        this.targetId = in.readString();
    }

    public static final Creator<FriendSource> CREATOR = new Creator<FriendSource>() {
        @Override
        public FriendSource createFromParcel(Parcel source) {
            return new FriendSource(source);
        }

        @Override
        public FriendSource[] newArray(int size) {
            return new FriendSource[size];
        }
    };
}
