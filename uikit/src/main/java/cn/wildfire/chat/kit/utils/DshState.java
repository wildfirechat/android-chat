/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.graphics.Color;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;

/**
 * DSH 会话运行时状态工具类（scope=31 会话级用户设置，type=1 状态 + type=2 Token 统计 +
 * type=3 AI 面板数据）。
 * <p>
 * 机器人把 {state, phase, toolName, model, ...} JSON 写到 key
 * {@code <convType>-<line>-<target>_1}（运行状态）；Token 统计
 * （usage/turn/context/cacheHitRatePct/speed/metricsAt）写到 type=2 独立通道
 * {@code <convType>-<line>-<target>_2}（回合结束必推，含出错/取消），两者独立推送。
 * AI 面板数据（组合查询结果：model 当前值+目录 / effort / sandbox / plan / cwd /
 * sessionId / dirs）写到 type=3 {@code <convType>-<line>-<target>_3}，面板打开/更新后刷新。
 * 群成员都能收到该会话级设置。
 * 标题栏、会话列表、输入面板共用此处的判定/读取/文案与颜色。
 * </p>
 * <p>
 * 面板（模型/推理等级/工作目录/沙箱/计划等）的当前值改从 type=3 读
 * （静默通道：面板打开发 207 DSH_Command query 组合查询，插件聚合写 type=3，
 * 不再发 /model /effort /ls 等文本命令、不再解析机器人回复文本）；type=1 仅提供
 * 运行状态与 lastChange（变更可见），统计计量文本走 type=2。
 * </p>
 * <p>
 * DSH/AI 会话判定统一按 {@code conversation.line == 2}（AI 消息使用 line 2：
 * 普通消息 line 0、朋友圈 line 1），不再依赖 UserInfo.type == 1 或群 extra 标记。
 * </p>
 */
public class DshState {
    public static final int DSH_STATE_TYPE = 1; // 1=状态 (业务约定)
    public static final int DSH_METRICS_TYPE = 2; // 2=Token 统计（独立通道，回合结束必推，含出错/取消）
    public static final int DSH_PANEL_TYPE = 3; // 3=AI 面板数据（207 query 组合查询结果，面板打开/更新后刷新）

    public static final String STATE_IDLE = "idle";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_WAITING_USER = "waiting_user";
    public static final String STATE_DONE = "done";

    private DshState() {
    }

    /**
     * scope=31 会话设置键前缀：{@code <convType>-<line>-<target>_<type>_}（含尾随 "_"）。
     * <p>
     * 服务端写入 scope=31 的键统一为 {@code <convType>-<line>-<target>_<type>_<机器人uid>}
     * （uid 后缀，不再写无后缀旧键），读取时以该前缀匹配会话内的 type 槽位。
     * </p>
     */
    public static String dshSettingKeyPrefix(Conversation conversation, int type) {
        return conversation.type.getValue() + "-" + conversation.line + "-" + conversation.target + "_" + type + "_";
    }

    /**
     * 读 scope=31 中当前会话指定 type 槽位的设置值（键带机器人 uid 后缀）。
     * <p>
     * 列出该 scope 全部会话设置（{@link ChatManager#getUserSettings(int)}），取首个键以
     * {@code <convType>-<line>-<target>_<type>_} 开头的条目（同一会话同 type 只应有一份，
     * 有多个 uid 前缀命中时取首个）。找不到/出错返回 null。仍由设置更新事件驱动刷新，不轮询。
     * </p>
     */
    private static String dshSettingValue(Conversation conversation, int type) {
        String prefix = dshSettingKeyPrefix(conversation, type);
        try {
            Map<String, String> settings = ChatManager.Instance().getUserSettings(UserSettingScope.Conversation_User_Setting);
            if (settings == null || settings.isEmpty()) {
                return null;
            }
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                if (entry.getKey() != null && entry.getKey().startsWith(prefix)) {
                    return entry.getValue();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * 群 extra 是否带 {"dsh":true} 标记（容错：非 JSON 时返回 false）。
     * <p>
     * 保留仅供旧数据/兼容场景查询；DSH 会话判定已统一改为按
     * {@code conversation.line == 2}（见 {@link #dshConversationKind}），
     * 不再以此作为判定依据。
     * </p>
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
     * AI/DSH 会话类型。判定依据统一为：群聊会话且 {@code conversation.line == 2}
     * （AI 消息统一使用 line 2；单聊是全局控制面板，不判 AI）。
     * 是 AI 会话时返回 "group"，否则返回 null。
     */
    public static String dshConversationKind(Conversation conversation) {
        if (conversation == null || TextUtils.isEmpty(conversation.target)) {
            return null;
        }
        // AI 会话 = 群聊 + line 2；单聊不判 AI（控制面板）
        if (conversation.type != Conversation.ConversationType.Group || conversation.line != 2) {
            return null;
        }
        return "group";
    }

    /**
     * 是否 AI/DSH 会话（群聊会话 line == 2）。非 AI 会话不查询/不展示 DSH 状态。
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
            String raw = dshSettingValue(conversation, DSH_STATE_TYPE);
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
     * 读取会话的 DSH Token 统计（scope=31 type=2 计量），未设置/非法/非 DSH 会话时返回 null。
     * <p>
     * 独立于运行状态（type=1）：回合结束必推（含出错/取消），带 metricsAt 时间戳，
     * 与状态推送互不依赖。返回对象含 usage/turn/context/cacheHitRatePct/speed/metricsAt 字段。
     * 面板（模型/推理等级/工作目录/沙箱/计划等）的当前值改从 type=3 读，不在此处。
     * </p>
     */
    public static JSONObject getDshMetrics(Conversation conversation) {
        if (!isDshConversation(conversation)) {
            return null;
        }
        try {
            String raw = dshSettingValue(conversation, DSH_METRICS_TYPE);
            if (TextUtils.isEmpty(raw)) {
                return null;
            }
            return new JSONObject(raw);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取会话的 AI 面板数据（scope=31 type=3 组合查询结果），未设置/非法/非 DSH 会话时返回 null。
     * <p>
     * 静默通道：面板打开时发 207 DSH_Command（op=query）组合查询，插件聚合面板数据
     * （model 当前值+目录 / effort / sandbox / plan / cwd / sessionId / dirs 根目录子目录）
     * 写入 type=3（不回复消息）；本端读 type=3 渲染面板，不解析机器人回复文本。
     * 返回对象结构：{@code {"model":{"current":"provider/id","options":[{"value":..,"label":..}]},
     * "effort":{"current":"high","options":["low","medium","high"]},
     * "sandbox":{"current":"workspace-write","options":["read-only","workspace-write","danger-full-access"]},
     * "plan":{"on":true},"cwd":"/abs/path","sessionId":"wildfire-...","dirs":["server","vue-pc-chat",...]}}
     * </p>
     */
    public static JSONObject getDshPanelData(Conversation conversation) {
        if (!isDshConversation(conversation)) {
            return null;
        }
        try {
            String raw = dshSettingValue(conversation, DSH_PANEL_TYPE);
            if (TextUtils.isEmpty(raw)) {
                return null;
            }
            return new JSONObject(raw);
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
     * 状态圆点颜色：running=主色 / waiting_user=#f59e0b / idle、done 及其他=#22c55e（绿，可继续指示任务）。
     */
    public static int stateColor(String state) {
        if (STATE_RUNNING.equals(state)) {
            return Color.parseColor("#3B62E0"); // 与 @color/colorPrimary 一致
        }
        if (STATE_WAITING_USER.equals(state)) {
            return Color.parseColor("#f59e0b");
        }
        // idle / done 及其他未知状态统一绿色：表示"可以输入继续指示任务"
        return Color.parseColor("#22c55e");
    }

    /**
     * 数字格式化：整数不带小数，小数保留 1 位（如 0.8、70.5、98）。
     */
    private static String fmtNum(double n) {
        if (Double.isNaN(n) || Double.isInfinite(n)) {
            return null;
        }
        if (n == Math.rint(n)) {
            return String.valueOf((long) n);
        }
        return String.valueOf(Math.round(n * 10) / 10.0);
    }

    /**
     * 读取 JSON 数值字段：仅当字段存在且为 number 类型时返回，否则返回 null（字符串/缺失一律视为无）。
     */
    private static Double optNumber(JSONObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        Object value = obj.opt(key);
        if (!(value instanceof Number)) {
            return null;
        }
        return ((Number) value).doubleValue();
    }

    /**
     * Token/上下文计量 → 一行展示文本（scope=31 type=2 统计对象）。
     * <p>
     * 输入为 {@link #getDshMetrics} 的返回值（type=2 独立通道，回合结束必推，含出错/取消）：
     * {@code context.usedPct / cacheHitRatePct / speed.tokensPerSec /
     * turn.outputTokens / usage.totalTokens}。
     * 只输出统计段（上下文 x% / 缓存 y% / z tok/s / 本轮 n tok / 累计 m tok）；
     * 运行态提示（等待确认/审批、错误、取消）走 {@link #dshStatusHint}（type=1），不在此处。
     * 各段用 " · " 连接；没有可展示的统计时返回空串。
     * </p>
     */
    public static String dshMetricsText(JSONObject metrics) {
        if (metrics == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();

        // 上下文占用（下一请求预估成本 / 模型窗口）
        JSONObject context = metrics.optJSONObject("context");
        Double usedPct = optNumber(context, "usedPct");
        if (usedPct != null) {
            parts.add("上下文 " + fmtNum(usedPct) + "%");
        }
        // 缓存命中率（累计口径）
        Double cacheHitRatePct = optNumber(metrics, "cacheHitRatePct");
        if (cacheHitRatePct != null) {
            parts.add("缓存 " + fmtNum(cacheHitRatePct) + "%");
        }
        // 本轮生成速度
        JSONObject speed = metrics.optJSONObject("speed");
        Double tokensPerSec = optNumber(speed, "tokensPerSec");
        if (tokensPerSec != null) {
            parts.add(fmtNum(tokensPerSec) + " tok/s");
        }
        // 本轮输出 token
        JSONObject turn = metrics.optJSONObject("turn");
        Double outputTokens = optNumber(turn, "outputTokens");
        if (outputTokens != null && outputTokens > 0) {
            parts.add("本轮 " + fmtNum(outputTokens) + " tok");
        }
        // 累计用量
        JSONObject usage = metrics.optJSONObject("usage");
        Double totalTokens = optNumber(usage, "totalTokens");
        if (totalTokens != null) {
            parts.add("累计 " + fmtNum(totalTokens) + " tok");
        }

        return TextUtils.join(" · ", parts);
    }

    /**
     * 运行态提示（scope=31 type=1 状态）→ 一段文本：
     * waiting_user → 🤔 等待确认 / 🔐 等待审批；reason=error → ⚠️ 错误；cancelled → 已取消。
     * <p>
     * 与 Token 统计（type=2，见 {@link #dshMetricsText}）分开输出：统计只出数字段，
     * 这里只出运行态提示；两者由调用方合并为标题一行（如 "AI 在线 · 🤔 等待确认 · 上下文 0.8%"）。
     * 无提示返回空串。
     * </p>
     */
    public static String dshStatusHint(JSONObject state) {
        if (state == null) {
            return "";
        }
        // 交互等待：优先提示在等什么
        if (STATE_WAITING_USER.equals(state.optString("state"))) {
            return "approval".equals(state.optString("interaction")) ? "🔐 等待审批" : "🤔 等待确认";
        }
        // 结果原因 / 错误
        String reason = state.optString("reason");
        if ("error".equals(reason)) {
            String error = state.optString("error");
            return "⚠️ " + (TextUtils.isEmpty(error) ? "出错了" : error);
        }
        if ("cancelled".equals(reason)) {
            return "已取消";
        }
        return "";
    }
}
