/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.dsh;

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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Dsh_Answer;

/**
 * DSH 结构化回答消息（用户→机器人），渲染时按摘要文本展示。
 * <p>
 * 消息类型: 201。
 * payload.content 为 JSON 字符串：{"qid":"uuid","answers":[{"id":"q1","selected":["是"],"custom":"可选补充"}]}
 * </p>
 */
@ContentTag(type = ContentType_Dsh_Answer, flag = PersistFlag.Persist)
public class DshAnswerMessageContent extends MessageContent {
    private String content;

    private JSONObject contentJson;

    public DshAnswerMessageContent() {
    }

    public DshAnswerMessageContent(String qid, JSONArray answers) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("qid", qid != null ? qid : "");
            jsonObject.put("answers", answers != null ? answers : new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.content = jsonObject.toString();
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
        JSONArray answers = getContentJson().optJSONArray("answers");
        List<String> parts = new ArrayList<>();
        if (answers != null) {
            for (int i = 0; i < answers.length(); i++) {
                JSONObject answer = answers.optJSONObject(i);
                if (answer == null) {
                    continue;
                }
                JSONArray selected = answer.optJSONArray("selected");
                List<String> labels = new ArrayList<>();
                if (selected != null) {
                    for (int j = 0; j < selected.length(); j++) {
                        labels.add(selected.optString(j));
                    }
                }
                if (!labels.isEmpty()) {
                    parts.add(TextUtils.join("、", labels));
                } else {
                    String custom = answer.optString("custom");
                    if (!TextUtils.isEmpty(custom)) {
                        parts.add(custom);
                    }
                }
            }
        }
        return parts.isEmpty() ? "（已作答）" : "已选择：" + TextUtils.join("；", parts);
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

    protected DshAnswerMessageContent(Parcel in) {
        super(in);
        this.content = in.readString();
    }

    public static final Creator<DshAnswerMessageContent> CREATOR = new Creator<DshAnswerMessageContent>() {
        @Override
        public DshAnswerMessageContent createFromParcel(Parcel source) {
            return new DshAnswerMessageContent(source);
        }

        @Override
        public DshAnswerMessageContent[] newArray(int size) {
            return new DshAnswerMessageContent[size];
        }
    };
}
