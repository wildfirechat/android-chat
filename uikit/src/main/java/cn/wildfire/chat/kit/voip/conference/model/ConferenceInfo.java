package cn.wildfire.chat.kit.voip.conference.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConferenceInfo {
    private String conferenceId;
    private String conferenceTitle;
    private String password;
    private String pin;
    private String owner;
    private List<String> managers;
    private String focus;
    // 秒
    long startTime;
    long endTime;
    boolean audience;
    boolean advance;
    @SerializedName("allowSwitchMode")
    boolean allowTurnOnMic;
    boolean noJoinBeforeStart;
    boolean recording;

    public String getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(String conferenceId) {
        this.conferenceId = conferenceId;
    }

    public String getConferenceTitle() {
        return conferenceTitle;
    }

    public void setConferenceTitle(String conferenceTitle) {
        this.conferenceTitle = conferenceTitle;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getManagers() {
        return managers;
    }

    public void setManagers(List<String> managers) {
        this.managers = managers;
    }

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isAudience() {
        return audience;
    }

    public void setAudience(boolean audience) {
        this.audience = audience;
    }

    public boolean isAdvance() {
        return advance;
    }

    public void setAdvance(boolean advance) {
        this.advance = advance;
    }

    public boolean isAllowTurnOnMic() {
        return allowTurnOnMic;
    }

    public void setAllowTurnOnMic(boolean allowTurnOnMic) {
        this.allowTurnOnMic = allowTurnOnMic;
    }

    public boolean isNoJoinBeforeStart() {
        return noJoinBeforeStart;
    }

    public void setNoJoinBeforeStart(boolean noJoinBeforeStart) {
        this.noJoinBeforeStart = noJoinBeforeStart;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    //    public Map<String, Object> toMap() {
//        HashMap<String, Object> map = new HashMap<>();
//        map.put("conferenceId", this.conferenceId);
//        map.put("conferenceTitle", this.conferenceTitle);
//        map.put("password", this.password);
//        map.put("pin", this.pin);
//        map.put("owner", this.owner);
//        map.put("startTime", this.startTime);
//        map.put("endTime", this.endTiem);
//        map.put("audience", this.audience);
//        map.put("advance", this.advance);
//        map.put("allowSwitchMode", this.allowSwitchMode);
//        map.put("noJoinBeforeStart", this.noJoinBeforeStart);
//        return map;
//    }
//
//    public static ConferenceInfo fromMap(Map<String, Object> map) {
//        ConferenceInfo info = new ConferenceInfo();
//        info.conferenceId = (String) map.get("conferenceId");
//        info.conferenceTitle = (String) map.get("conferenceTitle");
//        info.password = (String) map.get("password");
//        info.pin = (String) map.get("pin");
//        info.owner = (String) map.get("owner");
//        if (map.get("startTime") != null) {
//            info.startTime = (long) map.get("startTime");
//        }
//        if (map.get("endTime") != null) {
//            info.endTiem = (long) map.get("endTime");
//        }
//        return info;
//    }
}
