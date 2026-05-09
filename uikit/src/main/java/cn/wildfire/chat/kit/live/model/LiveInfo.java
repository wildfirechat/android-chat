package cn.wildfire.chat.kit.live.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;

public class LiveInfo implements Parcelable {
    private String host;
    private String liveId;
    private String pin;
    private String title;
    private String description;
    private String coverUrl;

    // 直播机器人 id，客户端可能会用来过滤参与者
    private String liveBotId;

    private String groupId;

    private String hlsUrl;
    private List<String> participants;

    private boolean audioOnly;

    private boolean audience;

    // 0 未开始；1，正在进行，已结束
    private int status;
    private long startTimestamp;

    public LiveInfo() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getLiveId() {
        return liveId;
    }

    public void setLiveId(String liveId) {
        this.liveId = liveId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

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

    public String getLiveBotId() {
        return liveBotId;
    }

    public void setLiveBotId(String liveBotId) {
        this.liveBotId = liveBotId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getHlsUrl() {
        return hlsUrl;
    }

    public void setHlsUrl(String hlsUrl) {
        this.hlsUrl = hlsUrl;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }


    public boolean isAudioOnly() {
        return audioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    public boolean isAudience() {
        return audience;
    }

    public void setAudience(boolean audience) {
        this.audience = audience;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    protected LiveInfo(Parcel in) {
        host = in.readString();
        liveId = in.readString();
        pin = in.readString();
        title = in.readString();
        description = in.readString();
        coverUrl = in.readString();
        liveBotId = in.readString();
        groupId = in.readString();
        hlsUrl = in.readString();
        participants = in.createStringArrayList();
        audioOnly = in.readByte() != 0;
        audience = in.readByte() != 0;
        status = in.readInt();
        startTimestamp = in.readLong();
    }

    public static final Creator<LiveInfo> CREATOR = new Creator<LiveInfo>() {
        @Override
        public LiveInfo createFromParcel(Parcel in) {
            return new LiveInfo(in);
        }

        @Override
        public LiveInfo[] newArray(int size) {
            return new LiveInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(host);
        dest.writeString(liveId);
        dest.writeString(pin);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(coverUrl);
        dest.writeString(liveBotId);
        dest.writeString(groupId);
        dest.writeString(hlsUrl);
        dest.writeStringList(participants);
        dest.writeByte((byte) (audioOnly ? 1 : 0));
        dest.writeByte((byte) (audience ? 1 : 0));
        dest.writeInt(status);
        dest.writeLong(startTimestamp);
    }
}
