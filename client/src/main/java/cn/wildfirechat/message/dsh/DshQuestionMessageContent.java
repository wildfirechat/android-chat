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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Dsh_Question;

/**
 * DSH 提问卡片消息（机器人→用户）。
 * <p>
 * 消息类型: 200（200-209 为官方预留 AI 交互段，与 PC 端/服务端已定稿）。
 * payload.content 为 JSON 字符串：
 * {"qid":"uuid","questions":[{"id":"q1","header":"...","question":"...","detail":"...",
 * "options":[{"label":"是"}],"multiSelect":false,"intent":{"kind":"plan-review","approve":"批准"}}],
 * "state":"pending"}
 * state ∈ pending/answered/expired，由机器人侧 updateMessage 更新。
 * </p>
 */
@ContentTag(type = ContentType_Dsh_Question, flag = PersistFlag.Persist)
public class DshQuestionMessageContent extends MessageContent {
    // 结构化数据原文（JSON 字符串），按需解析
    private String content;

    private JSONObject contentJson;

    public DshQuestionMessageContent() {
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

    public String getQid() {
        return getContentJson().optString("qid");
    }

    public JSONArray getQuestions() {
        return getContentJson().optJSONArray("questions");
    }

    public String getState() {
        return getContentJson().optString("state", "pending");
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
        JSONArray questions = getQuestions();
        JSONObject first = questions != null ? questions.optJSONObject(0) : null;
        if (first == null) {
            return "🤔 需要你确认";
        }
        String header = first.optString("header");
        String question = first.optString("question");
        return "🤔 " + (TextUtils.isEmpty(header) ? "" : "【" + header + "】") + question;
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

    protected DshQuestionMessageContent(Parcel in) {
        super(in);
        this.content = in.readString();
    }

    public static final Creator<DshQuestionMessageContent> CREATOR = new Creator<DshQuestionMessageContent>() {
        @Override
        public DshQuestionMessageContent createFromParcel(Parcel source) {
            return new DshQuestionMessageContent(source);
        }

        @Override
        public DshQuestionMessageContent[] newArray(int size) {
            return new DshQuestionMessageContent[size];
        }
    };
}
