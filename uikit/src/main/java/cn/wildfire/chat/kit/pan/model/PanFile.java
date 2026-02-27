/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * 网盘文件/文件夹
 */
public class PanFile implements Parcelable {
    
    /**
     * 文件类型
     */
    public enum FileType {
        FILE(1),    // 文件
        FOLDER(2);  // 文件夹
        
        private final int value;
        
        FileType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static FileType fromValue(int value) {
            for (FileType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return null;
        }
    }
    
    private Long id;
    private Long spaceId;
    private Long parentId;
    private String name;
    private Integer type;
    private Long size;
    private String mimeType;
    private String md5;
    private String storageUrl;
    private Integer childCount;
    private String creatorId;
    private String creatorName;
    private Long createdAt;
    private Long updatedAt;
    
    public PanFile() {
    }
    
    protected PanFile(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        if (in.readByte() == 0) {
            spaceId = null;
        } else {
            spaceId = in.readLong();
        }
        if (in.readByte() == 0) {
            parentId = null;
        } else {
            parentId = in.readLong();
        }
        name = in.readString();
        if (in.readByte() == 0) {
            type = null;
        } else {
            type = in.readInt();
        }
        if (in.readByte() == 0) {
            size = null;
        } else {
            size = in.readLong();
        }
        mimeType = in.readString();
        md5 = in.readString();
        storageUrl = in.readString();
        if (in.readByte() == 0) {
            childCount = null;
        } else {
            childCount = in.readInt();
        }
        creatorId = in.readString();
        creatorName = in.readString();
        if (in.readByte() == 0) {
            createdAt = null;
        } else {
            createdAt = in.readLong();
        }
        if (in.readByte() == 0) {
            updatedAt = null;
        } else {
            updatedAt = in.readLong();
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
        if (spaceId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(spaceId);
        }
        if (parentId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(parentId);
        }
        dest.writeString(name);
        if (type == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(type);
        }
        if (size == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(size);
        }
        dest.writeString(mimeType);
        dest.writeString(md5);
        dest.writeString(storageUrl);
        if (childCount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(childCount);
        }
        dest.writeString(creatorId);
        dest.writeString(creatorName);
        if (createdAt == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(createdAt);
        }
        if (updatedAt == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(updatedAt);
        }
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<PanFile> CREATOR = new Creator<PanFile>() {
        @Override
        public PanFile createFromParcel(Parcel in) {
            return new PanFile(in);
        }
        
        @Override
        public PanFile[] newArray(int size) {
            return new PanFile[size];
        }
    };
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getSpaceId() {
        return spaceId;
    }
    
    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getType() {
        return type;
    }
    
    public void setType(Integer type) {
        this.type = type;
    }
    
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String getMd5() {
        return md5;
    }
    
    public void setMd5(String md5) {
        this.md5 = md5;
    }
    
    public String getStorageUrl() {
        return storageUrl;
    }
    
    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }
    
    public Integer getChildCount() {
        return childCount;
    }
    
    public void setChildCount(Integer childCount) {
        this.childCount = childCount;
    }
    
    public String getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
    
    public String getCreatorName() {
        return creatorName;
    }
    
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 获取文件类型枚举
     */
    public FileType getFileTypeEnum() {
        return FileType.fromValue(type);
    }
    
    /**
     * 是否是文件夹
     */
    public boolean isFolder() {
        return type != null && type == 2;
    }
    
    /**
     * 是否是文件
     */
    public boolean isFile() {
        return type != null && type == 1;
    }
    
    /**
     * 从JSON对象解析
     */
    public static PanFile fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        PanFile file = new PanFile();
        file.id = json.optLong("id", 0);
        file.spaceId = json.optLong("spaceId", 0);
        file.parentId = json.optLong("parentId", 0);
        file.name = json.optString("name", null);
        file.size = json.optLong("size", 0);
        file.mimeType = json.optString("mimeType", null);
        file.md5 = json.optString("md5", null);
        file.storageUrl = json.optString("storageUrl", null);
        file.childCount = json.optInt("childCount", 0);
        file.creatorId = json.optString("creatorId", null);
        file.creatorName = json.optString("creatorName", null);
        file.createdAt = json.optLong("createdAt", 0);
        file.updatedAt = json.optLong("updatedAt", 0);
        
        // type 是字符串枚举，需要转换为整数
        String typeStr = json.optString("type", "");
        if ("FOLDER".equals(typeStr)) {
            file.type = FileType.FOLDER.getValue();
        } else {
            file.type = FileType.FILE.getValue();
        }
        
        return file;
    }
}
