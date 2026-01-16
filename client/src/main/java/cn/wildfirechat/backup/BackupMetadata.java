package cn.wildfirechat.backup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 备份元数据
 */
public class BackupMetadata {
    private String version;
    private String format;
    private String backupTime;
    private String userId;
    private String appType;
    private String backupMode;
    private String deviceName; // 设备名称
    private BackupEncryptionInfo encryption;
    private BackupStatistics statistics;
    private List<BackupConversationInfo> conversations;
    private String backupDir; // Backup directory name (e.g., "backup_2025-01-14_10-30-45")

    public BackupMetadata() {
        this.version = BackupConstants.BACKUP_VERSION;
        this.format = BackupConstants.BACKUP_FORMAT;
        this.appType = BackupConstants.BACKUP_APP_TYPE;
        this.backupMode = BackupConstants.BACKUP_MODE_MESSAGE_WITH_MEDIA;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("format", format);
        json.put("backupTime", backupTime);
        json.put("userId", userId);
        json.put("appType", appType);
        json.put("backupMode", backupMode);
        json.put("deviceName", deviceName != null ? deviceName : android.os.Build.MODEL);

        if (encryption != null) {
            json.put("encryption", encryption.toJSON());
        }

        if (statistics != null) {
            JSONObject statsJson = new JSONObject();
            statsJson.put("totalConversations", statistics.totalConversations);
            statsJson.put("totalMessages", statistics.totalMessages);
            statsJson.put("mediaFileCount", statistics.mediaFileCount);
            statsJson.put("mediaTotalSize", statistics.mediaTotalSize);
            if (statistics.firstMessageTime > 0) {
                JSONObject timeRange = new JSONObject();
                timeRange.put("firstMessageTime", statistics.firstMessageTime);
                timeRange.put("lastMessageTime", statistics.lastMessageTime);
                statsJson.put("timeRange", timeRange);
            }
            json.put("statistics", statsJson);
        }

        if (conversations != null && !conversations.isEmpty()) {
            JSONArray convArray = new JSONArray();
            for (BackupConversationInfo conv : conversations) {
                convArray.put(conv.toJSON());
            }
            json.put("conversations", convArray);
        }

        return json;
    }

    public static BackupMetadata fromJSON(JSONObject json) throws JSONException {
        BackupMetadata metadata = new BackupMetadata();
        metadata.version = json.optString("version", BackupConstants.BACKUP_VERSION);
        metadata.format = json.optString("format", BackupConstants.BACKUP_FORMAT);
        metadata.backupTime = json.optString("backupTime");
        metadata.userId = json.optString("userId");
        metadata.appType = json.optString("appType", BackupConstants.BACKUP_APP_TYPE);
        metadata.backupMode = json.optString("backupMode");
        metadata.deviceName = json.optString("deviceName", "Unknown Device");

        if (json.has("encryption")) {
            metadata.encryption = BackupEncryptionInfo.fromJSON(json.getJSONObject("encryption"));
        }

        if (json.has("statistics")) {
            JSONObject statsJson = json.getJSONObject("statistics");
            metadata.statistics = new BackupStatistics();
            metadata.statistics.totalConversations = statsJson.optInt("totalConversations", 0);
            metadata.statistics.totalMessages = statsJson.optInt("totalMessages", 0);
            metadata.statistics.mediaFileCount = statsJson.optInt("mediaFileCount", 0);
            metadata.statistics.mediaTotalSize = statsJson.optLong("mediaTotalSize", 0);

            if (statsJson.has("timeRange")) {
                JSONObject timeRange = statsJson.getJSONObject("timeRange");
                metadata.statistics.firstMessageTime = timeRange.optLong("firstMessageTime", 0);
                metadata.statistics.lastMessageTime = timeRange.optLong("lastMessageTime", 0);
            }
        }

        if (json.has("conversations")) {
            JSONArray convArray = json.getJSONArray("conversations");
            for (int i = 0; i < convArray.length(); i++) {
                if (metadata.conversations == null) {
                    metadata.conversations = new ArrayList<>();
                }
                metadata.conversations.add(BackupConversationInfo.fromJSON(convArray.getJSONObject(i)));
            }
        }

        return metadata;
    }

    // Getters and Setters
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(String backupTime) {
        this.backupTime = backupTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getBackupMode() {
        return backupMode;
    }

    public void setBackupMode(String backupMode) {
        this.backupMode = backupMode;
    }

    public BackupEncryptionInfo getEncryption() {
        return encryption;
    }

    public void setEncryption(BackupEncryptionInfo encryption) {
        this.encryption = encryption;
    }

    public BackupStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(BackupStatistics statistics) {
        this.statistics = statistics;
    }

    public List<BackupConversationInfo> getConversations() {
        return conversations;
    }

    public void setConversations(List<BackupConversationInfo> conversations) {
        this.conversations = conversations;
    }

    public String getBackupDir() {
        return backupDir;
    }

    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * 备份统计信息
     */
    public static class BackupStatistics {
        public int totalConversations;
        public int totalMessages;
        public int mediaFileCount;
        public long mediaTotalSize;
        public long firstMessageTime;
        public long lastMessageTime;
    }
}
