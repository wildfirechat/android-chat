package cn.wildfire.chat.kit.live.model;

public class CreateLiveRequest {
    private String title;
    private String description;
    // 封面
    private String coverUrl;
    // 群直播，仅先群成员观看时需要
    private String groupId;

    // 是否是观众模式，观众模式不允许连麦
    private boolean audience;
    // 是否进行录制
    private boolean record;
    // 最多连麦人数，包含自己
    private int maxParticipantCount;
    // 语音直播
    private boolean audioOnly;
    // 直播开始时间，0，立即开始
    private long startTime;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isAudience() {
        return audience;
    }

    public void setAudience(boolean audience) {
        this.audience = audience;
    }

    public boolean isRecord() {
        return record;
    }

    public void setRecord(boolean record) {
        this.record = record;
    }

    public int getMaxParticipantCount() {
        return maxParticipantCount;
    }

    public void setMaxParticipantCount(int maxParticipantCount) {
        this.maxParticipantCount = maxParticipantCount;
    }

    public boolean isAudioOnly() {
        return audioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
