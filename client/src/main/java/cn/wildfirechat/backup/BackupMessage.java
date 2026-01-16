package cn.wildfirechat.backup;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.core.MessagePayload;

/**
 * 备份消息模型
 */
public class BackupMessage {
    private long messageUid;
    private String fromUser;
    private String[] toUsers;
    private int direction;
    private int status;
    private long timestamp;
    private String localExtra;
    private BackupMessagePayload payload;
    private long mediaFileSize;

    public BackupMessage() {
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("messageUid", messageUid);
        json.put("fromUser", fromUser);
        if (toUsers != null && toUsers.length > 0) {
            json.put("toUsers", new org.json.JSONArray(toUsers));
        }
        json.put("direction", direction);
        json.put("status", status);
        json.put("timestamp", timestamp);
        json.put("localExtra", localExtra != null ? localExtra : "");
        if (payload != null) {
            json.put("payload", payload.toJSON());
        }
        json.put("mediaFileSize", mediaFileSize);
        return json;
    }

    public static BackupMessage fromJSON(JSONObject json) throws JSONException {
        BackupMessage message = new BackupMessage();
        message.messageUid = json.optLong("messageUid", 0);
        message.fromUser = json.optString("fromUser");

        if (json.has("toUsers")) {
            org.json.JSONArray toUsersArray = json.getJSONArray("toUsers");
            message.toUsers = new String[toUsersArray.length()];
            for (int i = 0; i < toUsersArray.length(); i++) {
                message.toUsers[i] = toUsersArray.optString(i);
            }
        }

        message.direction = json.optInt("direction", 0);
        message.status = json.optInt("status", 0);
        message.timestamp = json.optLong("timestamp", 0);
        message.localExtra = json.optString("localExtra", "");
        message.mediaFileSize = json.optLong("mediaFileSize", 0);

        if (json.has("payload")) {
            message.payload = BackupMessagePayload.fromJSON(json.getJSONObject("payload"));
        }

        return message;
    }

    // Getters and Setters
    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String[] getToUsers() {
        return toUsers;
    }

    public void setToUsers(String[] toUsers) {
        this.toUsers = toUsers;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLocalExtra() {
        return localExtra;
    }

    public void setLocalExtra(String localExtra) {
        this.localExtra = localExtra;
    }

    public BackupMessagePayload getPayload() {
        return payload;
    }

    public void setPayload(BackupMessagePayload payload) {
        this.payload = payload;
    }

    public long getMediaFileSize() {
        return mediaFileSize;
    }

    public void setMediaFileSize(long mediaFileSize) {
        this.mediaFileSize = mediaFileSize;
    }

    /**
     * 消息负载
     */
    public static class BackupMessagePayload {
        private int contentType;
        private String searchableContent;
        private String pushContent;
        private String pushData;
        private String content;
        private String binaryContent; // Base64 编码
        private String localContent;
        private int mentionedType;
        private String[] mentionedTargets;
        private String extra;
        private boolean notLoaded;
        private int mediaType;
        private String remoteMediaUrl;
        private BackupMediaInfo localMediaInfo;

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("contentType", contentType);
            json.put("searchableContent", searchableContent != null ? searchableContent : "");
            json.put("pushContent", pushContent != null ? pushContent : "");
            json.put("pushData", pushData != null ? pushData : "");
            json.put("content", content != null ? content : "");
            json.put("binaryContent", binaryContent != null ? binaryContent : "");
            json.put("localContent", localContent != null ? localContent : "");
            json.put("mentionedType", mentionedType);
            if (mentionedTargets != null && mentionedTargets.length > 0) {
                json.put("mentionedTargets", new org.json.JSONArray(mentionedTargets));
            }
            json.put("extra", extra != null ? extra : "");
            json.put("notLoaded", notLoaded);
            json.put("mediaType", mediaType);
            json.put("remoteMediaUrl", remoteMediaUrl != null ? remoteMediaUrl : "");
            if (localMediaInfo != null) {
                json.put("localMediaInfo", localMediaInfo.toJSON());
            }
            return json;
        }

        public static BackupMessagePayload fromJSON(JSONObject json) throws JSONException {
            BackupMessagePayload payload = new BackupMessagePayload();
            payload.contentType = json.optInt("contentType", 0);
            payload.searchableContent = json.optString("searchableContent", "");
            payload.pushContent = json.optString("pushContent", "");
            payload.pushData = json.optString("pushData", "");
            payload.content = json.optString("content", "");
            payload.binaryContent = json.optString("binaryContent", "");
            payload.localContent = json.optString("localContent", "");
            payload.mentionedType = json.optInt("mentionedType", 0);
            payload.notLoaded = json.optBoolean("notLoaded", false);
            payload.mediaType = json.optInt("mediaType", 0);
            payload.remoteMediaUrl = json.optString("remoteMediaUrl", "");
            payload.extra = json.optString("extra", "");

            if (json.has("mentionedTargets")) {
                org.json.JSONArray targetsArray = json.getJSONArray("mentionedTargets");
                payload.mentionedTargets = new String[targetsArray.length()];
                for (int i = 0; i < targetsArray.length(); i++) {
                    payload.mentionedTargets[i] = targetsArray.optString(i);
                }
            }

            if (json.has("localMediaInfo")) {
                payload.localMediaInfo = BackupMediaInfo.fromJSON(json.getJSONObject("localMediaInfo"));
            }

            return payload;
        }

        // Getters and Setters
        public int getContentType() {
            return contentType;
        }

        public void setContentType(int contentType) {
            this.contentType = contentType;
        }

        public String getSearchableContent() {
            return searchableContent;
        }

        public void setSearchableContent(String searchableContent) {
            this.searchableContent = searchableContent;
        }

        public String getPushContent() {
            return pushContent;
        }

        public void setPushContent(String pushContent) {
            this.pushContent = pushContent;
        }

        public String getPushData() {
            return pushData;
        }

        public void setPushData(String pushData) {
            this.pushData = pushData;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getBinaryContent() {
            return binaryContent;
        }

        public void setBinaryContent(String binaryContent) {
            this.binaryContent = binaryContent;
        }

        public String getLocalContent() {
            return localContent;
        }

        public void setLocalContent(String localContent) {
            this.localContent = localContent;
        }

        public int getMentionedType() {
            return mentionedType;
        }

        public void setMentionedType(int mentionedType) {
            this.mentionedType = mentionedType;
        }

        public String[] getMentionedTargets() {
            return mentionedTargets;
        }

        public void setMentionedTargets(String[] mentionedTargets) {
            this.mentionedTargets = mentionedTargets;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }

        public boolean isNotLoaded() {
            return notLoaded;
        }

        public void setNotLoaded(boolean notLoaded) {
            this.notLoaded = notLoaded;
        }

        public int getMediaType() {
            return mediaType;
        }

        public void setMediaType(int mediaType) {
            this.mediaType = mediaType;
        }

        public String getRemoteMediaUrl() {
            return remoteMediaUrl;
        }

        public void setRemoteMediaUrl(String remoteMediaUrl) {
            this.remoteMediaUrl = remoteMediaUrl;
        }

        public BackupMediaInfo getLocalMediaInfo() {
            return localMediaInfo;
        }

        public void setLocalMediaInfo(BackupMediaInfo localMediaInfo) {
            this.localMediaInfo = localMediaInfo;
        }

        /**
         * 转换为 MessagePayload（用于解码消息）
         * @return MessagePayload 对象
         */
        public MessagePayload toMessagePayload() {
            MessagePayload payload = new MessagePayload();
            payload.type = this.contentType;
            payload.searchableContent = this.searchableContent;
            payload.pushContent = this.pushContent;
            payload.pushData = this.pushData;
            payload.content = this.content;
            payload.binaryContent = this.binaryContent != null ? android.util.Base64.decode(this.binaryContent, android.util.Base64.NO_WRAP) : null;
            payload.localContent = this.localContent;
            payload.mentionedType = this.mentionedType;
            if (this.mentionedTargets != null) {
                payload.mentionedTargets = java.util.Arrays.asList(this.mentionedTargets);
            }
            payload.extra = this.extra;
            payload.notLoaded = this.notLoaded ? 1 : 0;
            payload.remoteMediaUrl = this.remoteMediaUrl;

            // 设置 mediaType
            if (this.mediaType >= 0 && this.mediaType < cn.wildfirechat.message.MessageContentMediaType.values().length) {
                payload.mediaType = cn.wildfirechat.message.MessageContentMediaType.values()[this.mediaType];
            }

            // 设置本地媒体信息
            if (this.localMediaInfo != null) {
                payload.localMediaPath = this.localMediaInfo.getRelativePath();
            }

            return payload;
        }

        /**
         * 从 MessagePayload 转换为 BackupMessagePayload
         * @param payload 原始 MessagePayload
         * @return 备份消息负载
         */
        public static BackupMessagePayload fromMessagePayload(MessagePayload payload) {
            BackupMessagePayload backupPayload = new BackupMessagePayload();

            backupPayload.setContentType(payload.type);
            backupPayload.setSearchableContent(payload.searchableContent);
            backupPayload.setPushContent(payload.pushContent);
            backupPayload.setPushData(payload.pushData);
            backupPayload.setContent(payload.content);
            if (payload.binaryContent != null) {
                backupPayload.setBinaryContent(android.util.Base64.encodeToString(payload.binaryContent, android.util.Base64.NO_WRAP));
            }
            backupPayload.setLocalContent(payload.localContent);
            backupPayload.setMentionedType(payload.mentionedType);
            if (payload.mentionedTargets != null && !payload.mentionedTargets.isEmpty()) {
                backupPayload.setMentionedTargets(payload.mentionedTargets.toArray(new String[0]));
            }
            backupPayload.setExtra(payload.extra);
            backupPayload.setNotLoaded(payload.notLoaded != 0);
            if (payload.mediaType != null) {
                backupPayload.setMediaType(payload.mediaType.ordinal());
            }
            backupPayload.setRemoteMediaUrl(payload.remoteMediaUrl);

            return backupPayload;
        }
    }
}
