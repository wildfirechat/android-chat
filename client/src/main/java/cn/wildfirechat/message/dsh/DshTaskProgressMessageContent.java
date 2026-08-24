/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.dsh;

import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Dsh_TaskProgress;

/**
 * DSH 任务进度卡片消息（机器人→用户），纯展示。
 * <p>
 * 消息类型: 208。
 * payload.content 为 JSON 字符串：
 * {"tasks":[{"kind":"subagent|job","id":"...","label":"可选",
 * "status":"running|done|failed|completed|killed","reason":"失败原因可选","updatedAt":123}],
 * "updatedAt":123}
 * 由插件首次 sendCard 发送、之后 updateMessage 原地更新。
 * </p>
 */
@ContentTag(type = ContentType_Dsh_TaskProgress, flag = PersistFlag.Persist)
public class DshTaskProgressMessageContent extends MessageContent {
    // 结构化数据原文（JSON 字符串），按需解析
    private String content;

    private JSONObject contentJson;

    public DshTaskProgressMessageContent() {
    }

    public JSONObject getContentJson() {
        if (contentJson == null) {
            try {
                contentJson = new JSONObject(content != null ? content : "{}");
            } catch (JSONException e) {
                contentJson = new JSONObject();
            }
        }
        return contentJson;
    }

    public JSONArray getTasks() {
        return getContentJson().optJSONArray("tasks");
    }

    public long getUpdatedAt() {
        return getContentJson().optLong("updatedAt", 0);
    }

    /**
     * 摘要角标文案（与 PC 端 DshTaskProgressContentView 一致）：
     * 共 N 个 · M 运行中 / 共 N 个 · F 失败 / 共 N 个 · 全部完成；空任务返回空串。
     */
    public String getSummary() {
        JSONArray tasks = getTasks();
        int total = tasks != null ? tasks.length() : 0;
        if (total == 0) {
            return "";
        }
        int running = 0;
        int failed = 0;
        for (int i = 0; i < total; i++) {
            JSONObject task = tasks.optJSONObject(i);
            if (task == null) {
                continue;
            }
            String status = task.optString("status");
            if ("running".equals(status)) {
                running++;
            } else if ("failed".equals(status)) {
                failed++;
            }
        }
        if (running > 0) {
            return "共 " + total + " 个 · " + running + " 运行中";
        }
        if (failed > 0) {
            return "共 " + total + " 个 · " + failed + " 失败";
        }
        return "共 " + total + " 个 · 全部完成";
    }

    /**
     * 会话列表摘要（searchableContent）：如 "🧩 任务 2（2 运行中）"。
     */
    @Override
    public String digest(Message message) {
        JSONArray tasks = getTasks();
        int total = tasks != null ? tasks.length() : 0;
        if (total == 0) {
            return "🧩 任务进度";
        }
        int running = 0;
        int failed = 0;
        for (int i = 0; i < total; i++) {
            JSONObject task = tasks.optJSONObject(i);
            if (task == null) {
                continue;
            }
            String status = task.optString("status");
            if ("running".equals(status)) {
                running++;
            } else if ("failed".equals(status)) {
                failed++;
            }
        }
        if (running > 0) {
            return "🧩 任务 " + total + "（" + running + " 运行中）";
        }
        if (failed > 0) {
            return "🧩 任务 " + total + "（" + failed + " 失败）";
        }
        return "🧩 任务 " + total + "（全部完成）";
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = content != null ? content : "{}";
        payload.searchableContent = digest(null);
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        this.content = payload.content;
        this.contentJson = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.content);
    }

    protected DshTaskProgressMessageContent(Parcel in) {
        super(in);
        this.content = in.readString();
    }

    public static final Creator<DshTaskProgressMessageContent> CREATOR = new Creator<DshTaskProgressMessageContent>() {
        @Override
        public DshTaskProgressMessageContent createFromParcel(Parcel source) {
            return new DshTaskProgressMessageContent(source);
        }

        @Override
        public DshTaskProgressMessageContent[] newArray(int size) {
            return new DshTaskProgressMessageContent[size];
        }
    };

    /**
     * 单任务解析模型，仅只读暴露协议字段，展示逻辑在 ViewHolder。
     */
    public static class Task {
        public final String kind;
        public final String id;
        public final String label;
        public final String status;
        public final String reason;
        public final long updatedAt;

        private Task(String kind, String id, String label, String status, String reason, long updatedAt) {
            this.kind = kind;
            this.id = id;
            this.label = label;
            this.status = status;
            this.reason = reason;
            this.updatedAt = updatedAt;
        }

        public static Task from(JSONObject json) {
            if (json == null) {
                return null;
            }
            return new Task(
                json.optString("kind"),
                json.optString("id"),
                json.optString("label"),
                json.optString("status"),
                json.optString("reason"),
                json.optLong("updatedAt", 0)
            );
        }

        /**
         * 标签或 id 短前缀（与 PC 端一致：超过 12 位取尾部 8 位，无 id 时兜底"子任务"）。
         */
        public String displayLabel() {
            if (!TextUtils.isEmpty(label)) {
                return label;
            }
            if (TextUtils.isEmpty(id)) {
                return "子任务";
            }
            return id.length() > 12 ? "子任务 " + id.substring(id.length() - 8) : id;
        }
    }
}
