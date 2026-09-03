/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.dsh;

import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Agent_Goal;

/**
 * DSH 目标进度卡片消息（机器人→用户），纯展示。
 * <p>
 * 消息类型: 206。
 * payload.content 为 JSON 字符串，v1：
 * {"gid":"...","objective":"...","phase":"active|paused|blocked|complete","roundsStarted":3}
 * ver:2（兼容读）：
 * {"ver":2,"gid":"...","title":"...","state":"...","stage":"round 3","updatedAt":..., ...}
 * （ver:2 仍会带 v1 字段 objective/phase/roundsStarted；展示时 title/state/stage
 * 仅在对应 v1 字段缺失时回退使用。）
 * </p>
 */
@ContentTag(type = ContentType_Agent_Goal, flag = PersistFlag.Persist)
public class AgentGoalMessageContent extends MessageContent {
    private String content;

    private JSONObject contentJson;

    public AgentGoalMessageContent() {
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

    public String getObjective() {
        return getContentJson().optString("objective");
    }

    public String getPhase() {
        return getContentJson().optString("phase", "active");
    }

    public int getRoundsStarted() {
        return getContentJson().optInt("roundsStarted", 0);
    }

    /** 目标标题（ver:2 字段），v1 无此字段时返回空串。 */
    public String getTitle() {
        return getContentJson().optString("title");
    }

    /** 目标状态（ver:2 字段 state），v1 无此字段时返回空串。 */
    public String getState() {
        return getContentJson().optString("state");
    }

    /** 阶段文本（ver:2 字段 stage，如 "round 3"），无则返回空串。 */
    public String getStage() {
        return getContentJson().optString("stage");
    }

    /**
     * 展示用标题：v1 {@code objective} 优先，缺失（ver:2 消息）时回退 {@code title}。
     */
    public String getDisplayTitle() {
        String objective = getObjective();
        return TextUtils.isEmpty(objective) ? getTitle() : objective;
    }

    /**
     * 展示用阶段/状态：v1 {@code phase} 优先，缺失（ver:2 消息）时回退 {@code state}；
     * 两者都无则回退 v1 默认值 "active"，保证旧逻辑不变。
     */
    public String getDisplayPhase() {
        JSONObject json = getContentJson();
        String phase = json.optString("phase", "");
        if (!TextUtils.isEmpty(phase)) {
            return phase;
        }
        String state = json.optString("state", "");
        return TextUtils.isEmpty(state) ? "active" : state;
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
    public String digest(Message message) {
        return "🎯 " + getDisplayTitle() + "（" + getDisplayPhase() + "，round " + getRoundsStarted() + "）";
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

    protected AgentGoalMessageContent(Parcel in) {
        super(in);
        this.content = in.readString();
    }

    public static final Creator<AgentGoalMessageContent> CREATOR = new Creator<AgentGoalMessageContent>() {
        @Override
        public AgentGoalMessageContent createFromParcel(Parcel source) {
            return new AgentGoalMessageContent(source);
        }

        @Override
        public AgentGoalMessageContent[] newArray(int size) {
            return new AgentGoalMessageContent[size];
        }
    };
}
