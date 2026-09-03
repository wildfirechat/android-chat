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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Agent_Approval_Result;

/**
 * DSH 审批结果消息（用户→机器人），渲染时按摘要文本展示。
 * <p>
 * 消息类型: 203。
 * payload.content 为 JSON 字符串：{"aid":"uuid","action":"approve"|"reject"}
 * </p>
 */
@ContentTag(type = ContentType_Agent_Approval_Result, flag = PersistFlag.Persist)
public class AgentApprovalResultMessageContent extends MessageContent {
    public static final String ACTION_APPROVE = "approve";
    public static final String ACTION_REJECT = "reject";

    private String content;

    private JSONObject contentJson;

    public AgentApprovalResultMessageContent() {
    }

    public AgentApprovalResultMessageContent(String aid, String action) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("aid", aid != null ? aid : "");
            jsonObject.put("action", action != null ? action : ACTION_REJECT);
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

    public String getAction() {
        return getContentJson().optString("action", ACTION_REJECT);
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
        return ACTION_APPROVE.equals(getAction()) ? "（已同意）" : "（已拒绝）";
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

    protected AgentApprovalResultMessageContent(Parcel in) {
        super(in);
        this.content = in.readString();
    }

    public static final Creator<AgentApprovalResultMessageContent> CREATOR = new Creator<AgentApprovalResultMessageContent>() {
        @Override
        public AgentApprovalResultMessageContent createFromParcel(Parcel source) {
            return new AgentApprovalResultMessageContent(source);
        }

        @Override
        public AgentApprovalResultMessageContent[] newArray(int size) {
            return new AgentApprovalResultMessageContent[size];
        }
    };
}
