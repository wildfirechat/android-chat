/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * 网盘空间
 */
public class PanSpace implements Parcelable {
    
    /**
     * 空间类型
     */
    public enum SpaceType {
        GLOBAL_PUBLIC(1),   // 全局公共空间
        USER_PUBLIC(2),     // 用户公共空间
        USER_PRIVATE(3);    // 用户私有空间
        
        private final int value;
        
        SpaceType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static SpaceType fromValue(int value) {
            for (SpaceType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return null;
        }
    }
    
    private Long id;
    private Integer spaceType;
    private String ownerId;
    private String name;
    private Long totalQuota;
    private Long usedQuota;
    private Integer fileCount;
    private Integer folderCount;
    
    public PanSpace() {
    }
    
    protected PanSpace(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        if (in.readByte() == 0) {
            spaceType = null;
        } else {
            spaceType = in.readInt();
        }
        ownerId = in.readString();
        name = in.readString();
        if (in.readByte() == 0) {
            totalQuota = null;
        } else {
            totalQuota = in.readLong();
        }
        if (in.readByte() == 0) {
            usedQuota = null;
        } else {
            usedQuota = in.readLong();
        }
        if (in.readByte() == 0) {
            fileCount = null;
        } else {
            fileCount = in.readInt();
        }
        if (in.readByte() == 0) {
            folderCount = null;
        } else {
            folderCount = in.readInt();
        }
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(id);
        }
        if (spaceType == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(spaceType);
        }
        dest.writeString(ownerId);
        dest.writeString(name);
        if (totalQuota == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(totalQuota);
        }
        if (usedQuota == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(usedQuota);
        }
        if (fileCount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(fileCount);
        }
        if (folderCount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(folderCount);
        }
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<PanSpace> CREATOR = new Creator<PanSpace>() {
        @Override
        public PanSpace createFromParcel(Parcel in) {
            return new PanSpace(in);
        }
        
        @Override
        public PanSpace[] newArray(int size) {
            return new PanSpace[size];
        }
    };
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getSpaceType() {
        return spaceType;
    }
    
    public void setSpaceType(Integer spaceType) {
        this.spaceType = spaceType;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Long getTotalQuota() {
        return totalQuota;
    }
    
    public void setTotalQuota(Long totalQuota) {
        this.totalQuota = totalQuota;
    }
    
    public Long getUsedQuota() {
        return usedQuota;
    }
    
    public void setUsedQuota(Long usedQuota) {
        this.usedQuota = usedQuota;
    }
    
    public Integer getFileCount() {
        return fileCount;
    }
    
    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }
    
    public Integer getFolderCount() {
        return folderCount;
    }
    
    public void setFolderCount(Integer folderCount) {
        this.folderCount = folderCount;
    }
    
    /**
     * 获取空间类型枚举
     */
    public SpaceType getSpaceTypeEnum() {
        return SpaceType.fromValue(spaceType);
    }
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        SpaceType type = getSpaceTypeEnum();
        if (type == null) {
            return name;
        }
        switch (type) {
            case GLOBAL_PUBLIC:
                return "全局公共空间";
            case USER_PUBLIC:
                return "我的公共空间";
            case USER_PRIVATE:
                return "我的私有空间";
            default:
                return name;
        }
    }
    
    /**
     * 获取使用百分比
     */
    public int getUsagePercent() {
        if (totalQuota == null || totalQuota == 0) {
            return 0;
        }
        return (int) (usedQuota * 100 / totalQuota);
    }
    
    /**
     * 从JSON对象解析
     */
    public static PanSpace fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        PanSpace space = new PanSpace();
        space.id = json.optLong("id", 0);
        space.ownerId = json.optString("ownerId", null);
        space.name = json.optString("name", null);
        space.totalQuota = json.optLong("totalQuota", 0);
        space.usedQuota = json.optLong("usedQuota", 0);
        space.fileCount = json.optInt("fileCount", 0);
        space.folderCount = json.optInt("folderCount", 0);
        
        // spaceType 是字符串枚举，需要转换为整数
        String typeStr = json.optString("spaceType", "");
        if ("GLOBAL_PUBLIC".equals(typeStr)) {
            space.spaceType = SpaceType.GLOBAL_PUBLIC.getValue();
        } else if ("USER_PUBLIC".equals(typeStr)) {
            space.spaceType = SpaceType.USER_PUBLIC.getValue();
        } else if ("USER_PRIVATE".equals(typeStr)) {
            space.spaceType = SpaceType.USER_PRIVATE.getValue();
        } else {
            space.spaceType = 0;
        }
        
        return space;
    }
}
