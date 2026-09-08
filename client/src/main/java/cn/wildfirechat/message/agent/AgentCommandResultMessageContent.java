/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.agent;

import android.os.Parcel;
import android.text.TextUtils;

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
 * （messageId==0，digest 返回空串）。作为 207 {@link AgentCommandMessageContent}
 * 指令的应答通道，当前仅承载 {@code op=dirs}（目录列表按需获取）。
 * payload.content 为 JSON 字符串，v1：
 * <pre>
 * {
 *   "ver": 1,
 *   "op": "dirs",
 *   "seq": 12345,                 // 回显请求 seq，客户端据此关联 pending
 *   "robotId": "robot_xxx_yyy",   // 可选，应答机器人（多机器人会话寻址）
 *   "cwd": "/abs/current/dir",    // 可选，机器人当前工作目录
 *   "root": "/abs/root",          // 可选，目录列表的根目录（dirs 的父目录）
 *   "dirs": ["a", "b"],           // 目录名（非全路径），按名称升序
 *   "total": 194,                 // 可选，总条数
 *   "truncated": false            // 可选，是否被截断
 * }
 * </pre>
 * 客户端必须按 {@code seq} 匹配 pending 请求；seq 不匹配或已超时的应答直接丢弃。
 * </p>
 */
@ContentTag(type = ContentType_Agent_Command_Result, flag = PersistFlag.Transparent)
public class AgentCommandResultMessageContent extends MessageContent {

    /** 协议版本，当前为 1 */
    private int ver = 1;
    /** 应答对应的指令 op（当前仅 dirs） */
    private String op;
    /** 回显请求的 seq，用于关联 pending 请求 */
    private long seq;
    /** 应答机器人 uid（多机器人会话寻址，可能为空） */
    private String robotId;
    /** 机器人当前工作目录（可能为空） */
    private String cwd;
    /** 目录列表根目录（dirs 的父目录，可能为空） */
    private String root;
    /** 目录名列表（非全路径，按名称升序；可能为空） */
    private List<String> dirs = new ArrayList<>();
    /** 总条数（可选，0 表示未提供） */
    private int total;
    /** 是否被截断（可选） */
    private boolean truncated;

    public AgentCommandResultMessageContent() {
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

    /** 目录名列表（非全路径）；无 dirs 时返回空列表，不会返回 null。 */
    public List<String> getDirs() {
        return dirs;
    }

    public int getTotal() {
        return total;
    }

    public boolean isTruncated() {
        return truncated;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        JSONObject json = new JSONObject();
        try {
            json.put("ver", ver);
            json.put("op", op != null ? op : "");
            json.put("seq", seq);
            if (!TextUtils.isEmpty(robotId)) {
                json.put("robotId", robotId);
            }
            if (!TextUtils.isEmpty(cwd)) {
                json.put("cwd", cwd);
            }
            if (!TextUtils.isEmpty(root)) {
                json.put("root", root);
            }
            JSONArray dirsJson = new JSONArray();
            if (dirs != null) {
                for (String dir : dirs) {
                    if (!TextUtils.isEmpty(dir)) {
                        dirsJson.put(dir);
                    }
                }
            }
            json.put("dirs", dirsJson);
            if (total > 0) {
                json.put("total", total);
            }
            json.put("truncated", truncated);
        } catch (JSONException e) {
            // JSONObject.put(String, Object) 不会抛 JSONException，理论不可达
        }
        payload.content = json.toString();
        // 与插件侧一致的可搜索摘要（透明消息不落库、不显示，仅作占位）
        payload.searchableContent = dirs != null && !dirs.isEmpty()
            ? "📂 AI 目录列表（" + dirs.size() + "）" : "";
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            JSONObject json = new JSONObject(payload.content != null ? payload.content : "{}");
            this.ver = json.optInt("ver", 1);
            this.op = json.optString("op");
            this.seq = json.optLong("seq");
            this.robotId = json.has("robotId") ? json.optString("robotId") : null;
            this.cwd = json.has("cwd") ? json.optString("cwd") : null;
            this.root = json.has("root") ? json.optString("root") : null;
            this.total = json.optInt("total", 0);
            this.truncated = json.optBoolean("truncated", false);
            List<String> decoded = new ArrayList<>();
            JSONArray dirsJson = json.optJSONArray("dirs");
            if (dirsJson != null) {
                for (int i = 0; i < dirsJson.length(); i++) {
                    String dir = dirsJson.optString(i);
                    if (!TextUtils.isEmpty(dir)) {
                        decoded.add(dir);
                    }
                }
            }
            this.dirs = decoded;
        } catch (JSONException e) {
            // 非 JSON 内容忽略（保持默认值）
        }
    }

    @Override
    public String digest(Message message) {
        // 透明消息摘要为空：不显示在消息流
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
    }

    protected AgentCommandResultMessageContent(Parcel in) {
        super(in);
        this.ver = in.readInt();
        this.op = in.readString();
        this.seq = in.readLong();
        this.robotId = in.readString();
        this.cwd = in.readString();
        this.root = in.readString();
        List<String> dirs = in.createStringArrayList();
        this.dirs = dirs != null ? dirs : new ArrayList<>();
        this.total = in.readInt();
        this.truncated = in.readByte() != 0;
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
