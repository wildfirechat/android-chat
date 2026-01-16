package cn.wildfirechat.backup;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 备份媒体文件信息
 */
public class BackupMediaInfo {
    private String relativePath;
    private String fileId;
    private long fileSize;
    private String md5;

    public BackupMediaInfo() {
    }

    public BackupMediaInfo(String relativePath, String fileId, long fileSize, String md5) {
        this.relativePath = relativePath;
        this.fileId = fileId;
        this.fileSize = fileSize;
        this.md5 = md5;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("relativePath", relativePath);
        json.put("fileId", fileId);
        json.put("fileSize", fileSize);
        json.put("md5", md5);
        return json;
    }

    public static BackupMediaInfo fromJSON(JSONObject json) throws JSONException {
        BackupMediaInfo info = new BackupMediaInfo();
        info.relativePath = json.optString("relativePath");
        info.fileId = json.optString("fileId");
        info.fileSize = json.optLong("fileSize", 0);
        info.md5 = json.optString("md5");
        return info;
    }

    // Getters and Setters
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
