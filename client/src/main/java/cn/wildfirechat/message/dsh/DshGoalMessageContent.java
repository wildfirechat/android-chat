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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Dsh_Goal;

/**
 * DSH 目标进度卡片消息（机器人→用户），纯展示。
 * <p>
 * 消息类型: 206。
 * payload.content 为 JSON 字符串：
 * {"gid":"...","objective":"...","phase":"active|paused|blocked|complete","roundsStarted":3}
 * </p>
 */
@ContentTag(type = ContentType_Dsh_Goal, flag = PersistFlag.Persist)
public class DshGoalMessageContent extends MessageContent {
    private String content;

    private JSONObject contentJson;

    public DshGoalMessageContent() {
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
        return "🎯 " + getObjective() + "（" + getPhase() + "，round " + getRoundsStarted() + "）";
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

    protected DshGoalMessageContent(Parcel in) {
        super(in);
        this.content = in.readString();
    }

    public static final Creator<DshGoalMessageContent> CREATOR = new Creator<DshGoalMessageContent>() {
        @Override
        public DshGoalMessageContent createFromParcel(Parcel source) {
            return new DshGoalMessageContent(source);
        }

        @Override
        public DshGoalMessageContent[] newArray(int size) {
            return new DshGoalMessageContent[size];
        }
    };
}
