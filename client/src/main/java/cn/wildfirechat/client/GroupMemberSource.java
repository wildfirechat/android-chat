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
 * 群成员来源
 */
public class GroupMemberSource implements Parcelable {
    /**
     * 未知
     */
    public static final int Type_Unknown = 0;
    /**
     * 搜索
     */
    public static final int Type_Search = 1;
    /**
     * 邀请，targetId 为邀请人的用户 id
     */
    public static final int Type_Invite = 2;
    /**
     * 二维码，targetId 为分享群二维码的用户 id
     */
    public static final int Type_QRCode = 3;
    /**
     * 群名片，targetId 为分享群名片的用户 id
     */
    public static int Type_Card = 4;

    public int type = Type_Unknown;
    public String targetId;

    public GroupMemberSource(int type, String targetId) {
        this.type = type;
        this.targetId = targetId;
    }

    public static String buildGroupMemberSourceExtra(int sourceType, String sourceTargetId) {
        JSONObject obj = new JSONObject();
        JSONObject source = new JSONObject();
        try {
            source.put("t", sourceType);
            if (!TextUtils.isEmpty(sourceTargetId)) {
                source.put("i", sourceTargetId);
            }
            obj.put("s", source);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return obj.toString();
    }

    public static GroupMemberSource getGroupMemberSource(String memberExtra) {
        if (TextUtils.isEmpty(memberExtra)) {
            return new GroupMemberSource(Type_Unknown, null);
        }
        try {
            JSONObject obj = new JSONObject(memberExtra);
            JSONObject s = obj.optJSONObject("s");
            if (s != null) {
                int type = s.optInt("t");
                String target = s.optString("i");
                return new GroupMemberSource(type, target);
            }
        } catch (JSONException e) {
            // do nothing
        }
        return new GroupMemberSource(Type_Unknown, null);
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

    protected GroupMemberSource(Parcel in) {
        this.type = in.readInt();
        this.targetId = in.readString();
    }

    public static final Creator<GroupMemberSource> CREATOR = new Creator<GroupMemberSource>() {
        @Override
        public GroupMemberSource createFromParcel(Parcel source) {
            return new GroupMemberSource(source);
        }

        @Override
        public GroupMemberSource[] newArray(int size) {
            return new GroupMemberSource[size];
        }
    };
}
