/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.dsh;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Dsh_Command;

/**
 * DSH_Command（207）AI 面板静默指令消息（用户→机器人）。
 * <p>
 * 透明消息（{@link PersistFlag#Transparent}）：不存储、不计未读、不在消息流显示
 * （digest 返回空串）。payload.content 为 JSON 字符串，形如：
 * {"op":"query"} 或 {"op":"set","cmd":"/model deepseek-official/xxx","seq":123}
 * <ul>
 *   <li>op=query：组合查询。插件聚合面板数据（model 当前值+目录 / effort / sandbox /
 *       plan / cwd / sessionId / dirs 根目录子目录）写入 scope=31 type=3
 *       （键为 convType-line-target_3），不回复消息；</li>
 *   <li>op=set：更新。cmd 为命令文本（/model /effort /cwd /sandbox /plan /compact
 *       /reset）。插件执行后写 type=1 状态 lastChange（如 "模型 → deepseek-official/deepseek-v4-pro"，
 *       变更可见）并刷新 type=3。</li>
 * </ul>
 * seq 为递增序号，用于防重复/幂等（参考 PC 端 DshCommandMessageContent）。
 * </p>
 */
@ContentTag(type = ContentType_Dsh_Command, flag = PersistFlag.Transparent)
public class DshCommandMessageContent extends MessageContent {

    /** op：query（组合查询）/ set（更新） */
    private String op;
    /** set 时的命令文本（如 "/model deepseek-official/xxx"）；query 时为空 */
    private String cmd;
    /** 递增序号，防重复/幂等 */
    private long seq;

    public DshCommandMessageContent() {
    }

    public DshCommandMessageContent(String op, String cmd, long seq) {
        this.op = op;
        this.cmd = cmd;
        this.seq = seq;
    }

    public String getOp() {
        return op;
    }

    public String getCmd() {
        return cmd;
    }

    public long getSeq() {
        return seq;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        JSONObject json = new JSONObject();
        try {
            json.put("op", op != null ? op : "");
            json.put("cmd", cmd != null ? cmd : "");
            json.put("seq", seq);
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
        try {
            JSONObject json = new JSONObject(payload.content != null ? payload.content : "{}");
            this.op = json.optString("op");
            this.cmd = json.optString("cmd");
            this.seq = json.optLong("seq");
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
        dest.writeString(this.op);
        dest.writeString(this.cmd);
        dest.writeLong(this.seq);
    }

    protected DshCommandMessageContent(Parcel in) {
        super(in);
        this.op = in.readString();
        this.cmd = in.readString();
        this.seq = in.readLong();
    }

    public static final Creator<DshCommandMessageContent> CREATOR = new Creator<DshCommandMessageContent>() {
        @Override
        public DshCommandMessageContent createFromParcel(Parcel source) {
            return new DshCommandMessageContent(source);
        }

        @Override
        public DshCommandMessageContent[] newArray(int size) {
            return new DshCommandMessageContent[size];
        }
    };
}
