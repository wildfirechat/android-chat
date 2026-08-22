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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Dsh_Approval;

/**
 * DSH 工具审批卡片消息（机器人→用户）。
 * <p>
 * 消息类型: 202。
 * payload.content 为 JSON 字符串：{"aid":"uuid","toolName":"bash","reason":"...","state":"pending"}
 * state ∈ pending/approved/rejected/expired，由机器人侧 updateMessage 更新。
 * </p>
 */
@ContentTag(type = ContentType_Dsh_Approval, flag = PersistFlag.Persist)
public class DshApprovalMessageContent extends MessageContent {
    private String content;

    private JSONObject contentJson;

    public DshApprovalMessageContent() {
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

    public String getAid() {
        return getContentJson().optString("aid");
    }

    public String getToolName() {
        return getContentJson().optString("toolName");
    }

    public String getReason() {
        return getContentJson().optString("reason");
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
        String toolName = getToolName();
        String reason = getReason();
        return "🔐 工具审批：" + (TextUtils.isEmpty(toolName) ? "工具" : toolName)
            + (TextUtils.isEmpty(reason) ? "" : "（" + reason + "）");
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

    protected DshApprovalMessageContent(Parcel in) {
        super(in);
        this.content = in.readString();
    }

    public static final Creator<DshApprovalMessageContent> CREATOR = new Creator<DshApprovalMessageContent>() {
        @Override
        public DshApprovalMessageContent createFromParcel(Parcel source) {
            return new DshApprovalMessageContent(source);
        }

        @Override
        public DshApprovalMessageContent[] newArray(int size) {
            return new DshApprovalMessageContent[size];
        }
    };
}
