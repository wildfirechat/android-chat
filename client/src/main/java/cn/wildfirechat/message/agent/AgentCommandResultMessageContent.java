/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.agent;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Agent_Command_Result;

/**
 * Agent_Command_Result（209）AI 面板指令应答消息（机器人→用户）。
 * <p>
 * 透明消息（{@link PersistFlag#Transparent}）：不存储、不计未读、不在消息流显示
 * （digest 返回空串）。payload.content 为 JSON 字符串，形如：
 * <ul>
 *   <li>op=sessions：{"ver":1,"op":"sessions","seq":123,"robotId":"robot_xxx",
 *       "sessionId":"wildfire-...","sessions":[{"id":"wildfire-...","cwd":"/abs/path",
 *       "createdAt":1234567890,"updatedAt":1234567890,"eventCount":12,"sizeBytes":1234,
 *       "current":true}],"total":3}</li>
 *   <li>op=switch：{"ver":1,"op":"switch","seq":124,"robotId":"robot_xxx","ok":true,
 *       "sessionId":"wildfire-...","cwd":"/abs/path"}
 *       （失败：{"ok":false,"error":"无权限切换会话"}）</li>
 *   <li>op=dirs：{"ver":1,"op":"dirs","seq":125,"robotId":"robot_xxx","cwd":"/abs/path",
 *       "root":"/abs/root","dirs":["a","b"],"total":2,"truncated":false}</li>
 * </ul>
 * seq 回显对应 207 Agent_Command 的 seq，客户端按「会话 + seq + robotId」关联 pending 请求，
 * 避免多机器人/多会话并发时串应答。decode 容错：非 JSON/字段缺失不抛异常，
 * dirs / sessions 默认空列表。
 * </p>
 */
@ContentTag(type = ContentType_Agent_Command_Result, flag = PersistFlag.Transparent)
public class AgentCommandResultMessageContent extends MessageContent {

    /** 载荷版本（当前 1） */
    public static final int VERSION = 1;

    /** op=sessions 应答里的会话列表项（一个 IM 会话在 DSH 侧的历史 session） */
    public static class AgentSessionItem {
        /** 会话标题（首条用户消息前 40 字；可能为空） */
        private String title;
        /** DSH session id（wildfire-&lt;会话哈希&gt;-&lt;目录哈希&gt;[-&lt;epoch&gt;]） */
        private String id;
        /** 该会话绑定的工作目录（绝对路径） */
        private String cwd;
        /** 创建时间（毫秒） */
        private long createdAt;
        /** 最近活跃时间（毫秒） */
        private long updatedAt;
        /** 事件条数（后端能提供时才有） */
        private long eventCount;
        /** 日志字节数 */
        private long sizeBytes;
        /** 是否为当前正在使用的 session */
        private boolean current;

        public AgentSessionItem() {
        }

        public AgentSessionItem(String id, String cwd, long createdAt, long updatedAt,
                                long eventCount, long sizeBytes, boolean current) {
            this.id = id;
            this.cwd = cwd;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.eventCount = eventCount;
            this.sizeBytes = sizeBytes;
            this.current = current;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public String getCwd() {
            return cwd;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        public long getEventCount() {
            return eventCount;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public boolean isCurrent() {
            return current;
        }
    }

    /** 载荷版本（当前 1） */
    private int ver = VERSION;
    /** 对应请求的 op：sessions / switch / dirs */
    private String op;
    /** 回显请求 207 的 seq（客户端按此关联 pending 请求） */
    private long seq;
    /** 目标机器人 uid（原样回显请求的 robotId；多机器人会话寻址） */
    private String robotId;
    /** 应答时机器人侧的工作目录（绝对路径） */
    private String cwd;
    /** 目录列表来源目录（项目根，绝对路径；dirs 是它的一级子目录） */
    private String root;
    /** 目录名数组（非全路径，按名称升序） */
    private List<String> dirs = new ArrayList<>();
    /** 截断前的目录总数 */
    private int total;
    /** 是否被插件截断（上限 3000） */
    private boolean truncated;
    /** op=sessions 应答：最近会话列表（最多 10 条，按最近活跃倒序） */
    private List<AgentSessionItem> sessions = new ArrayList<>();
    /** op=sessions/switch 应答：当前生效的 session id */
    private String sessionId;
    /** op=switch 应答：是否切换成功 */
    private boolean ok;
    /** op=switch 失败原因 */
    private String error;

    public AgentCommandResultMessageContent() {
    }

    public AgentCommandResultMessageContent(String op, long seq, String robotId) {
        this.op = op;
        this.seq = seq;
        this.robotId = robotId;
    }

    public int getVer() {
        return ver;
    }

    public String getOp() {
        return op;
    }

    public long getSeq() {
        return seq;
    }

    public String getRobotId() {
        return robotId;
    }

    public String getCwd() {
        return cwd;
    }

    public String getRoot() {
        return root;
    }

    public List<String> getDirs() {
        return dirs;
    }

    public int getTotal() {
        return total;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public List<AgentSessionItem> getSessions() {
        return sessions;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isOk() {
        return ok;
    }

    public String getError() {
        return error;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        JSONObject json = new JSONObject();
        try {
            json.put("ver", ver > 0 ? ver : VERSION);
            json.put("op", op != null ? op : "");
            json.put("seq", seq);
            if (robotId != null && !robotId.isEmpty()) {
                json.put("robotId", robotId);
            }
            if (cwd != null && !cwd.isEmpty()) {
                json.put("cwd", cwd);
            }
            if (root != null && !root.isEmpty()) {
                json.put("root", root);
            }
            JSONArray dirsArray = new JSONArray();
            if (dirs != null) {
                for (String dir : dirs) {
                    if (dir != null) {
                        dirsArray.put(dir);
                    }
                }
            }
            json.put("dirs", dirsArray);
            json.put("total", total);
            json.put("truncated", truncated);
            JSONArray sessionsArray = new JSONArray();
            if (sessions != null) {
                for (AgentSessionItem item : sessions) {
                    if (item == null) {
                        continue;
                    }
                    JSONObject session = new JSONObject();
                    session.put("title", item.title != null ? item.title : "");
                    session.put("id", item.id != null ? item.id : "");
                    session.put("cwd", item.cwd != null ? item.cwd : "");
                    session.put("createdAt", item.createdAt);
                    session.put("updatedAt", item.updatedAt);
                    session.put("eventCount", item.eventCount);
                    session.put("sizeBytes", item.sizeBytes);
                    session.put("current", item.current);
                    sessionsArray.put(session);
                }
            }
            json.put("sessions", sessionsArray);
            if (sessionId != null && !sessionId.isEmpty()) {
                json.put("sessionId", sessionId);
            }
            json.put("ok", ok);
            if (error != null && !error.isEmpty()) {
                json.put("error", error);
            }
        } catch (JSONException e) {
            // JSONObject.put(String, Object) 不会抛 JSONException，理论不可达
        }
        payload.content = json.toString();
        // 透明消息：无可搜索内容
        payload.searchableContent = "";
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        // 默认空值：content 非法时不抛异常，消费方按「无有效应答」处理
        this.ver = VERSION;
        this.op = "";
        this.seq = 0;
        this.robotId = "";
        this.cwd = "";
        this.root = "";
        this.dirs = new ArrayList<>();
        this.total = 0;
        this.truncated = false;
        this.sessions = new ArrayList<>();
        this.sessionId = "";
        this.ok = false;
        this.error = "";
        if (payload == null || payload.content == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject(payload.content);
            this.ver = json.optInt("ver", VERSION);
            this.op = json.optString("op");
            this.seq = json.optLong("seq");
            this.robotId = json.optString("robotId");
            this.cwd = json.optString("cwd");
            this.root = json.optString("root");
            JSONArray dirsArray = json.optJSONArray("dirs");
            if (dirsArray != null) {
                for (int i = 0; i < dirsArray.length(); i++) {
                    String dir = dirsArray.optString(i);
                    if (dir != null && !dir.isEmpty()) {
                        this.dirs.add(dir);
                    }
                }
            }
            this.total = json.optInt("total");
            this.truncated = json.optBoolean("truncated");
            JSONArray sessionsArray = json.optJSONArray("sessions");
            if (sessionsArray != null) {
                for (int i = 0; i < sessionsArray.length(); i++) {
                    JSONObject session = sessionsArray.optJSONObject(i);
                    if (session == null) {
                        continue;
                    }
                    AgentSessionItem item = new AgentSessionItem(
                        session.optString("id"),
                        session.optString("cwd"),
                        session.optLong("createdAt"),
                        session.optLong("updatedAt"),
                        session.optLong("eventCount"),
                        session.optLong("sizeBytes"),
                        session.optBoolean("current"));
                    item.setTitle(session.optString("title"));
                    this.sessions.add(item);
                }
            }
            this.sessionId = json.optString("sessionId");
            this.ok = json.optBoolean("ok");
            this.error = json.optString("error");
        } catch (JSONException e) {
            // 非 JSON 内容忽略（保持默认值）
        }
    }

    @Override
    public String digest(Message message) {
        // 透明消息摘要为空：不显示在消息流，也不产生会话列表预览/通知
        return "";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.ver);
        dest.writeString(this.op);
        dest.writeLong(this.seq);
        dest.writeString(this.robotId);
        dest.writeString(this.cwd);
        dest.writeString(this.root);
        dest.writeStringList(this.dirs);
        dest.writeInt(this.total);
        dest.writeByte(this.truncated ? (byte) 1 : (byte) 0);
        dest.writeInt(this.sessions.size());
        for (AgentSessionItem item : this.sessions) {
            dest.writeString(item.getTitle());
            dest.writeString(item.getId());
            dest.writeString(item.getCwd());
            dest.writeLong(item.getCreatedAt());
            dest.writeLong(item.getUpdatedAt());
            dest.writeLong(item.getEventCount());
            dest.writeLong(item.getSizeBytes());
            dest.writeByte(item.isCurrent() ? (byte) 1 : (byte) 0);
        }
        dest.writeString(this.sessionId);
        dest.writeByte(this.ok ? (byte) 1 : (byte) 0);
        dest.writeString(this.error);
    }

    protected AgentCommandResultMessageContent(Parcel in) {
        super(in);
        this.ver = in.readInt();
        this.op = in.readString();
        this.seq = in.readLong();
        this.robotId = in.readString();
        this.cwd = in.readString();
        this.root = in.readString();
        this.dirs = in.createStringArrayList();
        if (this.dirs == null) {
            this.dirs = new ArrayList<>();
        }
        this.total = in.readInt();
        this.truncated = in.readByte() != 0;
        int size = in.readInt();
        this.sessions = new ArrayList<>(size > 0 ? size : 0);
        for (int i = 0; i < size; i++) {
            String title = in.readString();
            AgentSessionItem parcelItem = new AgentSessionItem(
                in.readString(),
                in.readString(),
                in.readLong(),
                in.readLong(),
                in.readLong(),
                in.readLong(),
                in.readByte() != 0);
            parcelItem.setTitle(title);
            this.sessions.add(parcelItem);
        }
        this.sessionId = in.readString();
        this.ok = in.readByte() != 0;
        this.error = in.readString();
    }

    public static final Creator<AgentCommandResultMessageContent> CREATOR = new Creator<AgentCommandResultMessageContent>() {
        @Override
        public AgentCommandResultMessageContent createFromParcel(Parcel source) {
            return new AgentCommandResultMessageContent(source);
        }

        @Override
        public AgentCommandResultMessageContent[] newArray(int size) {
            return new AgentCommandResultMessageContent[size];
        }
    };
}
