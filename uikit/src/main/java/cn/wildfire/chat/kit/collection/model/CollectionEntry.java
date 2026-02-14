/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.collection.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * UI层接龙参与条目模型
 * <p>
 * 与服务端的CollectionEntry对应，用于UI层展示。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class CollectionEntry {
    /**
     * 条目ID
     */
    private long entryId;
    /**
     * 接龙ID
     */
    private long collectionId;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 参与内容
     */
    private String content;
    /**
     * 创建时间（毫秒时间戳）
     */
    private long createdAt;
    /**
     * 更新时间（毫秒时间戳）
     */
    private long updatedAt;
    /**
     * 是否已删除：0=未删除，1=已删除
     */
    private int deleted;

    public CollectionEntry() {
    }

    public long getEntryId() {
        return entryId;
    }

    public void setEntryId(long entryId) {
        this.entryId = entryId;
    }

    public long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(long collectionId) {
        this.collectionId = collectionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    /**
     * 从JSON对象解析条目数据
     *
     * @param jsonObject JSON对象
     * @return CollectionEntry实例
     */
    public static CollectionEntry fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        CollectionEntry entry = new CollectionEntry();
        // 后端返回的是"id"而不是"entryId"
        entry.entryId = jsonObject.optLong("id", 0);
        entry.collectionId = jsonObject.optLong("collectionId", 0);
        entry.userId = jsonObject.optString("userId", "");
        entry.content = jsonObject.optString("content", "");
        entry.createdAt = jsonObject.optLong("createdAt", 0);
        entry.updatedAt = jsonObject.optLong("updatedAt", 0);
        entry.deleted = jsonObject.optInt("deleted", 0);
        return entry;
    }

    /**
     * 转换为JSON对象
     *
     * @return JSON对象
     * @throws JSONException JSON异常
     */
    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("entryId", entryId);
        jsonObject.put("collectionId", collectionId);
        jsonObject.put("userId", userId);
        jsonObject.put("content", content);
        jsonObject.put("createdAt", createdAt);
        jsonObject.put("updatedAt", updatedAt);
        jsonObject.put("deleted", deleted);
        return jsonObject;
    }
}
