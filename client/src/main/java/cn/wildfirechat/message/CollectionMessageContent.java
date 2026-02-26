/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Collection;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 接龙消息内容类
 * <p>
 * 用于表示接龙类型的消息内容，支持群聊中的接龙活动。
 * 与iOS的WFCCCollectionMessageContent对应。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_Collection, flag = PersistFlag.Persist_And_Count)
public class CollectionMessageContent extends MessageContent {
    /**
     * 接龙ID（字符串形式）
     */
    private String collectionId;
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
     * 参与模板（如：姓名-电话）
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
     * 参与记录列表
     */
    private List<CollectionEntry> entries;
    /**
     * 创建时间（毫秒时间戳）
     */
    private long createdAt;
    /**
     * 更新时间（毫秒时间戳）
     */
    private long updatedAt;

    public CollectionMessageContent() {
        this.entries = new ArrayList<>();
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
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

    public List<CollectionEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<CollectionEntry> entries) {
        this.entries = entries;
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

    /**
     * 获取参与人数
     *
     * @return 参与人数
     */
    public int getParticipantCount() {
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

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        // title放入searchableContent用于搜索
        payload.searchableContent = title;

        // 其他数据JSON编码后放入binaryContent
        JSONObject dataDict = new JSONObject();
        try {
            dataDict.put("collectionId", collectionId != null ? collectionId : "");
            dataDict.put("groupId", groupId != null ? groupId : "");
            dataDict.put("creatorId", creatorId != null ? creatorId : "");
            dataDict.put("desc", desc != null ? desc : "");
            dataDict.put("template", template != null ? template : "");
            dataDict.put("expireType", expireType);
            dataDict.put("expireAt", expireAt);
            dataDict.put("maxParticipants", maxParticipants);
            dataDict.put("status", status);
            dataDict.put("createdAt", createdAt);
            dataDict.put("updatedAt", updatedAt);

            // 编码参与记录
            if (entries != null && !entries.isEmpty()) {
                JSONArray entriesArray = new JSONArray();
                for (CollectionEntry entry : entries) {
                    entriesArray.put(entry.toJson());
                }
                dataDict.put("entries", entriesArray);
            }

            payload.binaryContent = dataDict.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        // 从searchableContent解析title
        this.title = payload.searchableContent;

        // 从binaryContent解析其他数据
        if (payload.binaryContent != null && payload.binaryContent.length > 0) {
            try {
                JSONObject dataDict = new JSONObject(new String(payload.binaryContent));
                this.collectionId = dataDict.optString("collectionId", "");
                this.groupId = dataDict.optString("groupId", "");
                this.creatorId = dataDict.optString("creatorId", "");
                this.desc = dataDict.optString("desc", "");
                this.template = dataDict.optString("template", "");
                this.expireType = dataDict.optInt("expireType", 0);
                this.expireAt = dataDict.optLong("expireAt", 0);
                this.maxParticipants = dataDict.optInt("maxParticipants", 0);
                this.status = dataDict.optInt("status", 0);
                this.createdAt = dataDict.optLong("createdAt", 0);
                this.updatedAt = dataDict.optLong("updatedAt", 0);

                // 解析参与记录
                this.entries = new ArrayList<>();
                if (dataDict.has("entries")) {
                    JSONArray entriesArray = dataDict.optJSONArray("entries");
                    if (entriesArray != null) {
                        for (int i = 0; i < entriesArray.length(); i++) {
                            JSONObject entryJson = entriesArray.optJSONObject(i);
                            if (entryJson != null) {
                                CollectionEntry entry = CollectionEntry.fromJson(entryJson);
                                if (entry != null) {
                                    this.entries.add(entry);
                                }
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String digest(Message message) {
        if (title != null && !title.isEmpty()) {
            return "[接龙] " + title;
        }
        return "[接龙]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.collectionId);
        dest.writeString(this.groupId);
        dest.writeString(this.creatorId);
        dest.writeString(this.title);
        dest.writeString(this.desc);
        dest.writeString(this.template);
        dest.writeInt(this.expireType);
        dest.writeLong(this.expireAt);
        dest.writeInt(this.maxParticipants);
        dest.writeInt(this.status);
        dest.writeTypedList(this.entries);
        dest.writeLong(this.createdAt);
        dest.writeLong(this.updatedAt);
    }

    protected CollectionMessageContent(Parcel in) {
        super(in);
        this.collectionId = in.readString();
        this.groupId = in.readString();
        this.creatorId = in.readString();
        this.title = in.readString();
        this.desc = in.readString();
        this.template = in.readString();
        this.expireType = in.readInt();
        this.expireAt = in.readLong();
        this.maxParticipants = in.readInt();
        this.status = in.readInt();
        this.entries = in.createTypedArrayList(CollectionEntry.CREATOR);
        this.createdAt = in.readLong();
        this.updatedAt = in.readLong();
    }

    public static final Creator<CollectionMessageContent> CREATOR = new Creator<CollectionMessageContent>() {
        @Override
        public CollectionMessageContent createFromParcel(Parcel source) {
            return new CollectionMessageContent(source);
        }

        @Override
        public CollectionMessageContent[] newArray(int size) {
            return new CollectionMessageContent[size];
        }
    };
}
