/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.collection.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * UI层接龙模型
 * <p>
 * 与服务端的CollectionMessageContent对应，用于UI层展示接龙详情。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class Collection {
    /**
     * 接龙ID
     */
    private long collectionId;
    /**
     * 群ID
     */
    private String groupId;
    /**
     * 创建者ID
     */
    private String creatorId;
    /**
     * 接龙标题
     */
    private String title;
    /**
     * 接龙描述
     */
    private String desc;
    /**
     * 参与模板
     */
    private String template;
    /**
     * 过期类型：0=无限期，1=有限期
     */
    private int expireType;
    /**
     * 过期时间（毫秒时间戳）
     */
    private long expireAt;
    /**
     * 最大参与人数（0表示无限制）
     */
    private int maxParticipants;
    /**
     * 状态：0=进行中，1=已结束，2=已取消
     */
    private int status;
    /**
     * 创建时间（毫秒时间戳）
     */
    private long createdAt;
    /**
     * 更新时间（毫秒时间戳）
     */
    private long updatedAt;
    /**
     * 参与记录列表
     */
    private List<CollectionEntry> entries;
    /**
     * 参与人数（服务器返回的统计值）
     */
    private int participantCount;

    public Collection() {
        this.entries = new ArrayList<>();
    }

    public long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(long collectionId) {
        this.collectionId = collectionId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public int getExpireType() {
        return expireType;
    }

    public void setExpireType(int expireType) {
        this.expireType = expireType;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(long expireAt) {
        this.expireAt = expireAt;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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

    public List<CollectionEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<CollectionEntry> entries) {
        this.entries = entries;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    /**
     * 计算实际的参与人数（排除已删除的记录）
     *
     * @return 参与人数
     */
    public int calculateParticipantCount() {
        if (entries == null) {
            return 0;
        }
        int count = 0;
        for (CollectionEntry entry : entries) {
            if (entry.getDeleted() == 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取当前用户的参与记录
     *
     * @param userId 用户ID
     * @return 参与记录，未找到返回null
     */
    public CollectionEntry getEntryForUser(String userId) {
        if (entries == null || userId == null) {
            return null;
        }
        for (CollectionEntry entry : entries) {
            if (userId.equals(entry.getUserId()) && entry.getDeleted() == 0) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 检查用户是否已参与
     *
     * @param userId 用户ID
     * @return true=已参与
     */
    public boolean hasUserJoined(String userId) {
        return getEntryForUser(userId) != null;
    }

    /**
     * 检查接龙是否已过期
     *
     * @return true=已过期
     */
    public boolean isExpired() {
        if (expireType == 0) {
            return false; // 无限期
        }
        return System.currentTimeMillis() > expireAt;
    }

    /**
     * 检查接龙是否已达到最大参与人数
     *
     * @return true=已满
     */
    public boolean isFull() {
        if (maxParticipants <= 0) {
            return false; // 无限制
        }
        return calculateParticipantCount() >= maxParticipants;
    }

    /**
     * 检查接龙是否可参与
     *
     * @return true=可参与
     */
    public boolean isJoinable() {
        return status == 0 && !isExpired() && !isFull();
    }

    /**
     * 从JSON对象解析接龙数据
     *
     * @param jsonObject JSON对象
     * @return Collection实例
     */
    public static Collection fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        Collection collection = new Collection();
        // 后端返回的是"id"而不是"collectionId"
        collection.collectionId = jsonObject.optLong("id", 0);
        collection.groupId = jsonObject.optString("groupId", "");
        collection.creatorId = jsonObject.optString("creatorId", "");
        collection.title = jsonObject.optString("title", "");
        // 后端返回的是"description"而不是"desc"
        collection.desc = jsonObject.optString("description", "");
        collection.template = jsonObject.optString("template", "");
        collection.expireType = jsonObject.optInt("expireType", 0);
        collection.expireAt = jsonObject.optLong("expireAt", 0);
        collection.maxParticipants = jsonObject.optInt("maxParticipants", 0);
        collection.status = jsonObject.optInt("status", 0);
        collection.createdAt = jsonObject.optLong("createdAt", 0);
        collection.updatedAt = jsonObject.optLong("updatedAt", 0);
        collection.participantCount = jsonObject.optInt("participantCount", 0);

        // 解析参与记录
        if (jsonObject.has("entries")) {
            JSONArray entriesArray = jsonObject.optJSONArray("entries");
            if (entriesArray != null) {
                for (int i = 0; i < entriesArray.length(); i++) {
                    JSONObject entryJson = entriesArray.optJSONObject(i);
                    if (entryJson != null) {
                        CollectionEntry entry = CollectionEntry.fromJson(entryJson);
                        if (entry != null) {
                            collection.entries.add(entry);
                        }
                    }
                }
            }
        }

        return collection;
    }

    /**
     * 转换为JSON对象
     *
     * @return JSON对象
     * @throws JSONException JSON异常
     */
    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("collectionId", collectionId);
        jsonObject.put("groupId", groupId);
        jsonObject.put("creatorId", creatorId);
        jsonObject.put("title", title);
        jsonObject.put("desc", desc);
        jsonObject.put("template", template);
        jsonObject.put("expireType", expireType);
        jsonObject.put("expireAt", expireAt);
        jsonObject.put("maxParticipants", maxParticipants);
        jsonObject.put("status", status);
        jsonObject.put("createdAt", createdAt);
        jsonObject.put("updatedAt", updatedAt);
        jsonObject.put("participantCount", participantCount);

        // 编码参与记录
        if (entries != null && !entries.isEmpty()) {
            JSONArray entriesArray = new JSONArray();
            for (CollectionEntry entry : entries) {
                entriesArray.put(entry.toJson());
            }
            jsonObject.put("entries", entriesArray);
        }

        return jsonObject;
    }
}
