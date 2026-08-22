/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.graphics.Color;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;

/**
 * DSH 会话运行时状态工具类（scope=31 会话级用户设置，type=1 状态）。
 * <p>
 * 机器人把 {state, phase, toolName, model, ...} JSON 写到 key
 * {@code <convType>-<line>-<target>_1}；群成员都能收到该会话级设置。
 * 标题栏、会话列表、输入面板共用此处的判定/读取/文案与颜色。
 * </p>
 */
public class DshState {
    public static final int DSH_STATE_TYPE = 1; // 1=状态 (业务约定)

    public static final String STATE_IDLE = "idle";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_WAITING_USER = "waiting_user";
    public static final String STATE_DONE = "done";

    private DshState() {
    }

    public static String dshStateKey(Conversation conversation) {
        return conversation.type.getValue() + "-" + conversation.line + "-" + conversation.target + "_" + DSH_STATE_TYPE;
    }

    /**
     * 群 extra 是否带 {"dsh":true} 标记（容错：非 JSON 时返回 false）。
     */
    public static boolean isDshGroupExtra(String extra) {
        if (TextUtils.isEmpty(extra)) {
            return false;
        }
        try {
            return new JSONObject(extra).optBoolean("dsh", false);
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * DSH 会话类型："single"（单聊且对方是机器人 UserInfo.type == 1）/
     * "group"（群 extra 带 {"dsh":true} 标记）/ null（非 DSH 会话）。
     * 用户信息/群信息本地未缓存时返回 null，待用户信息/群信息更新事件后重新判定。
     */
    public static String dshConversationKind(Conversation conversation) {
        if (conversation == null || TextUtils.isEmpty(conversation.target)) {
            return null;
        }
        try {
            if (conversation.type == Conversation.ConversationType.Single) {
                UserInfo userInfo = ChatManager.Instance().getUserInfo(conversation.target, false);
                return userInfo != null && userInfo.type == 1 ? "single" : null;
            }
            if (conversation.type == Conversation.ConversationType.Group) {
                GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(conversation.target, false);
                return groupInfo != null && isDshGroupExtra(groupInfo.extra) ? "group" : null;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * 是否 DSH 会话。非 DSH 会话不查询/不展示 DSH 状态。
     */
    public static boolean isDshConversation(Conversation conversation) {
        return dshConversationKind(conversation) != null;
    }

    /**
     * 读取会话的 DSH 运行时状态，未设置/非法/非 DSH 会话时返回 null。
     */
    public static JSONObject getDshState(Conversation conversation) {
        if (!isDshConversation(conversation)) {
            return null;
        }
        try {
            String raw = ChatManager.Instance().getUserSetting(UserSettingScope.Conversation_User_Setting, dshStateKey(conversation));
            if (TextUtils.isEmpty(raw)) {
                return null;
            }
            JSONObject state = new JSONObject(raw);
            return TextUtils.isEmpty(state.optString("state")) ? null : state;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 状态文案：空闲/运行中/等待确认/已完成；phase=="tool" 时追加" · {toolName}"（无 toolName 用"工具"）。
     */
    public static String stateText(JSONObject state) {
        if (state == null) {
            return null;
        }
        String label;
        switch (state.optString("state")) {
            case STATE_IDLE:
                label = "空闲";
                break;
            case STATE_RUNNING:
                label = "运行中";
                break;
            case STATE_WAITING_USER:
                label = "等待确认";
                break;
            case STATE_DONE:
                label = "已完成";
                break;
            default:
                return null;
        }
        if ("tool".equals(state.optString("phase"))) {
            String toolName = state.optString("toolName");
            label += " · " + (TextUtils.isEmpty(toolName) ? "工具" : toolName);
        }
        return label;
    }

    /**
     * 状态圆点颜色：running=主色 / waiting_user=#f59e0b / done=#22c55e / 其他=#94a3b8。
     */
    public static int stateColor(String state) {
        if (STATE_RUNNING.equals(state)) {
            return Color.parseColor("#3B62E0"); // 与 @color/colorPrimary 一致
        }
        if (STATE_WAITING_USER.equals(state)) {
            return Color.parseColor("#f59e0b");
        }
        if (STATE_DONE.equals(state)) {
            return Color.parseColor("#22c55e");
        }
        return Color.parseColor("#94a3b8");
    }
}
