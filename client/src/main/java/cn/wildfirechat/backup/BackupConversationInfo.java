package cn.wildfirechat.backup;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.model.Conversation;

/**
 * 备份会话信息
 */
public class BackupConversationInfo {
    private String conversationId;
    private Conversation.ConversationType type;
    private String target;
    private int line;
    private int messageCount;
    private int mediaCount;
    private String directory;
    private long firstMessageTime;
    private long lastMessageTime;

    // 会话设置
    private boolean isTop;
    private boolean isSilent;
    private String draft;

    public BackupConversationInfo() {
        this.line = 0;
    }

    public BackupConversationInfo(Conversation conversation, String directory) {
        this.type = conversation.type;
        this.target = conversation.target;
        this.line = conversation.line;
        this.directory = directory;
        this.conversationId = buildConversationId(type, target, line);
    }

    public static String buildConversationId(Conversation.ConversationType type, String target, int line) {
        return String.format("conv_type%d_%s_line%d", type.getValue(), target, line);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("conversationId", conversationId);
        json.put("type", type.getValue());
        json.put("target", target);
        json.put("line", line);
        json.put("messageCount", messageCount);
        json.put("mediaCount", mediaCount);
        json.put("directory", directory);
        if (firstMessageTime > 0) {
            json.put("firstMessageTime", firstMessageTime);
        }
        if (lastMessageTime > 0) {
            json.put("lastMessageTime", lastMessageTime);
        }
        return json;
    }

    public static BackupConversationInfo fromJSON(JSONObject json) throws JSONException {
        BackupConversationInfo info = new BackupConversationInfo();
        info.conversationId = json.optString("conversationId");
        info.type = Conversation.ConversationType.type(json.optInt("type", 0));
        info.target = json.optString("target");
        info.line = json.optInt("line", 0);
        info.messageCount = json.optInt("messageCount", 0);
        info.mediaCount = json.optInt("mediaCount", 0);
        info.directory = json.optString("directory");
        info.firstMessageTime = json.optLong("firstMessageTime", 0);
        info.lastMessageTime = json.optLong("lastMessageTime", 0);
        return info;
    }

    // Getters and Setters
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Conversation.ConversationType getType() {
        return type;
    }

    public void setType(Conversation.ConversationType type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getMediaCount() {
        return mediaCount;
    }

    public void setMediaCount(int mediaCount) {
        this.mediaCount = mediaCount;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public long getFirstMessageTime() {
        return firstMessageTime;
    }

    public void setFirstMessageTime(long firstMessageTime) {
        this.firstMessageTime = firstMessageTime;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isTop() {
        return isTop;
    }

    public void setTop(boolean top) {
        isTop = top;
    }

    public boolean isSilent() {
        return isSilent;
    }

    public void setSilent(boolean silent) {
        isSilent = silent;
    }

    public String getDraft() {
        return draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
    }
}
