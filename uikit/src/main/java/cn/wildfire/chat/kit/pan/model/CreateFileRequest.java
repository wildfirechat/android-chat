/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.model;

/**
 * 创建文件请求
 */
public class CreateFileRequest {
    private Long spaceId;
    private Long parentId;
    private String name;
    private Long size;
    private String mimeType;
    private String md5;
    private String storageUrl;
    private Boolean copy;
    
    public CreateFileRequest() {
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
    
    public Boolean getCopy() {
        return copy;
    }
    
    public void setCopy(Boolean copy) {
        this.copy = copy;
    }
}
