/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * 群组搜索结果类
 * <p>
 * 用于表示群组搜索的结果信息。
 * 包含群组信息、匹配类型和匹配成员列表。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class GroupSearchResult implements Parcelable {
    /**
     * 群组搜索匹配类型掩码接口
     */
    public interface GroupSearchMarchTypeMask {
        /**
         * 匹配群名称
         */
        int Group_Name_Mask = 0x01;

        /**
         * 匹配成员用户名
         */
        int Member_Name_Mask = 0x02;

        /**
         * 匹配群成员别名
         */
        int Member_Alias_Mask = 0x04;

        /**
         * 匹配群备注
         */
        int Group_Remark_Mask = 0x08;
    }

    /**
     * 群组信息
     */
    public GroupInfo groupInfo;

    /**
     * 匹配类型（GroupSearchMarchTypeMask）
     */
    public int marchedType;

    /**
     * 匹配的成员ID列表
     */
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
