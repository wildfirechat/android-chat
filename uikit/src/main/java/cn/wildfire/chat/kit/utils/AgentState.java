/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.graphics.Color;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;

/**
 * Agent 会话运行时状态工具类（scope=31 会话级用户设置，type=1 状态 + type=2 Token 统计 +
 * type=3 AI 面板数据）。
 * <p>
 * 机器人把 {state, phase, toolName, model, ...} JSON 写到 key
 * {@code <convType>-<line>-<target>_1}（运行状态）；Token 统计
 * （usage/turn/context/cacheHitRatePct/speed/metricsAt）写到 type=2 独立通道
 * {@code <convType>-<line>-<target>_2}（回合结束必推，含出错/取消），两者独立推送。
 * AI 面板数据（组合查询结果：model 当前值 / effort / sandbox / plan / cwd /
 * sessionId / preset / approval / mode；目录候选 dirs 已改走 209 按需应答，见 getAgentPanelData 注释）
 * 写到 type=3 {@code <convType>-<line>-<target>_3}，面板打开/更新后刷新。
 * 群成员都能收到该会话级设置。
 * 标题栏、会话列表、输入面板共用此处的判定/读取/文案与颜色。
 * </p>
 * <p>
 * 面板（模型/推理等级/工作目录/沙箱/计划等）的当前值改从 type=3 读
 * （静默通道：面板打开发 207 Agent_Command query 组合查询，插件聚合写 type=3，
 * 不再发 /model /effort /ls 等文本命令、不再解析机器人回复文本）；type=1 仅提供
 * 运行状态与 lastChange（变更可见），统计计量文本走 type=2。
 * </p>
 * <p>
 * Agent/AI 会话判定统一按 {@code conversation.line == 2}（AI 消息使用 line 2：
 * 普通消息 line 0、朋友圈 line 1），不再依赖 UserInfo.type == 1 或群 extra 标记。
 * </p>
 */
public class AgentState {
    public static final int Agent_STATE_TYPE = 1; // 1=状态 (业务约定)
    public static final int Agent_METRICS_TYPE = 2; // 2=Token 统计（独立通道，回合结束必推，含出错/取消）
    public static final int Agent_PANEL_TYPE = 3; // 3=AI 面板数据（207 query 组合查询结果，面板打开/更新后刷新）

    public static final String STATE_IDLE = "idle";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_WAITING_USER = "waiting_user";
    public static final String STATE_DONE = "done";

    private AgentState() {
    }

    /**
     * scope=31 会话设置键前缀：{@code <convType>-<line>-<target>_<type>_}（含尾随 "_"）。
     * <p>
     * 服务端写入 scope=31 的键统一为 {@code <convType>-<line>-<target>_<type>_<机器人uid>}
     * （uid 后缀，不再写无后缀旧键），读取时以该前缀匹配会话内的 type 槽位。
     * </p>
     */
    public static String agentSettingKeyPrefix(Conversation conversation, int type) {
        return conversation.type.getValue() + "-" + conversation.line + "-" + conversation.target + "_" + type + "_";
    }

    /**
     * 读 scope=31 中当前会话指定 type 槽位的设置值（键带机器人 uid 后缀）。
     * <p>
     * 按 key 前缀查询该 scope 的设置表（{@link ChatManager#getUserSettingsLike(int, String)}，
     * native 只返回 key 以 prefix 开头的条目，避免拉全量再自行筛选）；同一会话同 type 只应有
     * 一份，有多个 uid 前缀命中时取首个条目。找不到/出错返回 null。仍由设置更新事件驱动刷新，不轮询。
     * </p>
     */
    private static String agentSettingValue(Conversation conversation, int type) {
        String prefix = agentSettingKeyPrefix(conversation, type);
        try {
            Map<String, String> settings = ChatManager.Instance().getUserSettingsLike(UserSettingScope.Conversation_User_Setting, prefix);
            if (settings == null || settings.isEmpty()) {
                return null;
            }
            return settings.entrySet().iterator().next().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * AI/Agent 会话类型。判定依据统一为：群聊会话且 {@code conversation.line == 2}
     * （AI 消息统一使用 line 2；单聊是全局控制面板，不判 AI）。
     * 是 AI 会话时返回 "group"，否则返回 null。
     */
    public static String agentConversationKind(Conversation conversation) {
        if (conversation == null || TextUtils.isEmpty(conversation.target)) {
            return null;
        }
        // AI 会话 = 群聊 + line 2；单聊不判 AI（控制面板）
        if (conversation.type != Conversation.ConversationType.Group || conversation.line != Conversation.LINE_AGENT) {
            return null;
        }
        return "group";
    }

    /**
     * 是否 AI/Agent 会话（群聊会话 line == 2）。非 AI 会话不查询/不展示 Agent 状态。
     */
    public static boolean isAgentConversation(Conversation conversation) {
        return agentConversationKind(conversation) != null;
    }

    /**
     * 读取会话的 Agent 运行时状态，未设置/非法/非 Agent 会话时返回 null。
     */
    public static JSONObject getAgentState(Conversation conversation) {
        if (!isAgentConversation(conversation)) {
            return null;
        }
        try {
            String raw = agentSettingValue(conversation, Agent_STATE_TYPE);
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
     * 读取会话的 Agent Token 统计（scope=31 type=2 计量），未设置/非法/非 Agent 会话时返回 null。
     * <p>
     * 独立于运行状态（type=1）：回合结束必推（含出错/取消），带 metricsAt 时间戳，
     * 与状态推送互不依赖。返回对象含 usage/turn/context/cacheHitRatePct/speed/metricsAt 字段。
     * 面板（模型/推理等级/工作目录/沙箱/计划等）的当前值改从 type=3 读，不在此处。
     * </p>
     */
    public static JSONObject getAgentMetrics(Conversation conversation) {
        if (!isAgentConversation(conversation)) {
            return null;
        }
        try {
            String raw = agentSettingValue(conversation, Agent_METRICS_TYPE);
            if (TextUtils.isEmpty(raw)) {
                return null;
            }
            return new JSONObject(raw);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取会话的 AI 面板数据（scope=31 type=3 组合查询结果），未设置/非法/非 Agent 会话时返回 null。
     * <p>
     * 静默通道：面板打开时发 207 Agent_Command（op=query）组合查询，插件聚合面板数据
     * （model 当前值 / effort / sandbox / plan / cwd / sessionId / preset / approval / mode）
     * 写入 type=3（不回复消息）；本端读 type=3 渲染面板，不解析机器人回复文本。
     * 目录候选 {@code dirs} 已从 type=3 移除（v2.3，避免 scope=31 单值超限），
     * 改为面板发 207 op=dirs、插件用 209 Agent_Command_Result 透明消息回传；
     * 老插件仍带 {@code dirs} 时客户端直接使用（兼容）。
     * 返回对象结构：{@code {"model":{"current":"provider/id","options":[{"value":..,"label":..}]},
     * "effort":{"current":"high","options":["low","medium","high"]},
     * "sandbox":{"current":"workspace-write","options":["read-only","workspace-write","danger-full-access"]},
     * "plan":{"on":true},"cwd":"/abs/path","sessionId":"wildfire-...",
     * "preset":{"current":"standard","options":[{"value":"standard","label":"标准模式"}]},
     * "approval":{"current":"ask","options":[{"value":"ask","label":"询问（需批准的操作弹卡片）"}]},
     * "mode":{"current":"interrupt","options":[{"value":"interrupt","label":"打断（后到打断先到）"},
     * {"value":"queue","label":"排队（串行等待）"}]}}
     * （{@code preset.options} 可能为空数组=部署未提供 preset 服务；{@code preset}/{@code approval}
     * 可能整体缺失=旧插件，客户端容错为禁用控件并仅显示 current；
     * {@code mode} 可能整体缺失=旧插件，客户端回退本地兜底两项 interrupt/queue（控件仍可切换）；
     * 老插件可能仍含 {@code "dirs":["server","vue-pc-chat",...]}）
     * </p>
     */
    public static JSONObject getAgentPanelData(Conversation conversation) {
        if (!isAgentConversation(conversation)) {
            return null;
        }
        try {
            String raw = agentSettingValue(conversation, Agent_PANEL_TYPE);
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
     * 输入为 {@link #getAgentMetrics} 的返回值（type=2 独立通道，回合结束必推，含出错/取消）：
     * {@code context.usedPct / cacheHitRatePct / speed.tokensPerSec /
     * turn.outputTokens / usage.totalTokens}。
     * 只输出统计段（上下文 x% / 缓存 y% / z tok/s / 本轮 n tok / 累计 m tok）；
     * 运行态提示（等待确认/审批、错误、取消）走 {@link #agentStatusHint}（type=1），不在此处。
     * 各段用 " · " 连接；没有可展示的统计时返回空串。
     * </p>
     */
    public static String agentMetricsText(JSONObject metrics) {
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
     * 与 Token 统计（type=2，见 {@link #agentMetricsText}）分开输出：统计只出数字段，
     * 这里只出运行态提示；两者由调用方合并为标题一行（如 "AI 在线 · 🤔 等待确认 · 上下文 0.8%"）。
     * 无提示返回空串。
     * </p>
     */
    public static String agentStatusHint(JSONObject state) {
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

    // ===================== 多机器人（多 agent）支持 =====================

    /**
     * 会话内单个机器人的（type=1 状态 + type=2 计量）分组。
     */
    public static class RobotState {
        /** 完整机器人 uid（形如 robot_xxx_yyy，保持完整不截断） */
        public final String uid;
        /** type=1 运行状态 JSON（state/phase/toolName/lastChange/sessionId/...），有效（state 非空） */
        public final JSONObject state;
        /** type=2 Token 计量 JSON；该机器人无计量推送时为 null */
        public final JSONObject metrics;

        RobotState(String uid, JSONObject state, JSONObject metrics) {
            this.uid = uid;
            this.state = state;
            this.metrics = metrics;
        }

        public String getUid() {
            return uid;
        }

        public JSONObject getState() {
            return state;
        }

        public JSONObject getMetrics() {
            return metrics;
        }
    }

    /**
     * 机器人成员 id 判定：以 {@code robot_} / {@code robot-} 开头（兼容大小写，参考 PC 端
     * {@code /^robot[-_]/i}）。机器人是服务端的特殊用户，uid 由服务端下发，不做任何截断。
     */
    public static boolean isRobotUid(String uid) {
        if (TextUtils.isEmpty(uid)) {
            return false;
        }
        String lower = uid.toLowerCase();
        return lower.startsWith("robot_") || lower.startsWith("robot-");
    }

    /**
     * 机器人显示名：取该机器人（特殊用户）的用户信息显示名；取不到时回退完整 uid
     * （不做 {@code _} 截断，robot_xxx_yyy 多段 id 原样返回）。与 PC 端
     * {@code agentRobotName} 语义一致：displayName → name → uid。
     *
     * @param uid 完整机器人 uid；空串返回空串
     */
    public static String agentRobotName(String uid) {
        if (TextUtils.isEmpty(uid)) {
            return "";
        }
        try {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(uid, false);
            if (userInfo != null) {
                if (!TextUtils.isEmpty(userInfo.displayName)) {
                    return userInfo.displayName;
                }
                if (!TextUtils.isEmpty(userInfo.name)) {
                    return userInfo.name;
                }
            }
        } catch (Exception e) {
            // 用户信息读取失败时按回退处理
        }
        return uid;
    }

    /**
     * 列出会话内机器人 uid（完整 uid）列表。
     * <p>
     * 主来源：按 key 前缀 {@code <convType>-<line>-<target>_1_} 查询 scope=31 本地设置表
     * （{@link ChatManager#getUserSettingsLike(int, String)}），键后缀即机器人 uid——只有推送过
     * 状态的机器人会出现。
     * 兜底：群聊会话额外按“机器人成员”（memberId 以 robot_/robot- 开头）补齐，
     * 让尚未推送过状态、但已在群里的机器人也能出现在选择列表。
     * </p>
     *
     * @return 排好序的完整 uid 列表；非 AI 会话/空会话返回空列表
     */
    public static List<String> listAgentRobotUids(Conversation conversation) {
        List<String> result = new ArrayList<>();
        if (conversation == null || TextUtils.isEmpty(conversation.target)) {
            return result;
        }
        Set<String> uids = new LinkedHashSet<>();
        String prefix = agentSettingKeyPrefix(conversation, Agent_STATE_TYPE);
        try {
            Map<String, String> settings = ChatManager.Instance().getUserSettingsLike(UserSettingScope.Conversation_User_Setting, prefix);
            if (settings != null) {
                for (Map.Entry<String, String> entry : settings.entrySet()) {
                    String key = entry.getKey();
                    if (key != null && key.startsWith(prefix)) {
                        String uid = key.substring(prefix.length());
                        if (!TextUtils.isEmpty(uid)) {
                            uids.add(uid);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 设置表读取失败忽略（已有 scope=31 条目的机器人仍可列出）
        }
        // 兜底：群聊按“机器人成员”补齐
        if (conversation.type == Conversation.ConversationType.Group) {
            try {
                List<GroupMember> members = ChatManager.Instance().getGroupMembers(conversation.target, false);
                if (members != null) {
                    for (GroupMember member : members) {
                        if (member != null && isRobotUid(member.memberId)) {
                            uids.add(member.memberId);
                        }
                    }
                }
            } catch (Exception e) {
                // 群成员读取失败忽略（已有 scope31 条目的机器人仍可列出）
            }
        }
        result.addAll(uids);
        Collections.sort(result);
        return result;
    }

    /**
     * 读取会话内每个机器人的（type=1 状态 + type=2 计量），按机器人分组。
     * <p>
     * 只返回“有有效状态”的机器人（type=1 状态 JSON 存在且 state 字段非空），
     * 无状态的 uid 不展示（与 PC 端 getAgentRobotStates 一致）。
     * </p>
     *
     * @return [{uid, state, metrics}]，按 uid 排序；无条目/非 Agent 会话返回空列表
     */
    public static List<RobotState> getAgentRobotStates(Conversation conversation) {
        List<RobotState> result = new ArrayList<>();
        if (conversation == null || TextUtils.isEmpty(conversation.target) || !isAgentConversation(conversation)) {
            return result;
        }
        Map<String, JSONObject> stateByUid = new java.util.HashMap<>();
        Map<String, JSONObject> metricsByUid = new java.util.HashMap<>();
        Set<String> uids = new LinkedHashSet<>();
        String p1 = agentSettingKeyPrefix(conversation, Agent_STATE_TYPE);
        String p2 = agentSettingKeyPrefix(conversation, Agent_METRICS_TYPE);
        try {
            // type=1 状态 / type=2 计量分别按前缀查询（native 已过滤，避免拉全量再筛选）
            Map<String, String> stateSettings = ChatManager.Instance().getUserSettingsLike(UserSettingScope.Conversation_User_Setting, p1);
            if (stateSettings != null) {
                for (Map.Entry<String, String> entry : stateSettings.entrySet()) {
                    String key = entry.getKey();
                    if (key == null || !key.startsWith(p1)) {
                        continue;
                    }
                    String uid = key.substring(p1.length());
                    if (TextUtils.isEmpty(uid)) {
                        continue;
                    }
                    uids.add(uid);
                    JSONObject value = parseSettingJson(entry.getValue());
                    if (value != null) {
                        stateByUid.put(uid, value);
                    }
                }
            }
            Map<String, String> metricsSettings = ChatManager.Instance().getUserSettingsLike(UserSettingScope.Conversation_User_Setting, p2);
            if (metricsSettings != null) {
                for (Map.Entry<String, String> entry : metricsSettings.entrySet()) {
                    String key = entry.getKey();
                    if (key == null || !key.startsWith(p2)) {
                        continue;
                    }
                    String uid = key.substring(p2.length());
                    if (TextUtils.isEmpty(uid)) {
                        continue;
                    }
                    uids.add(uid);
                    JSONObject value = parseSettingJson(entry.getValue());
                    if (value != null) {
                        metricsByUid.put(uid, value);
                    }
                }
            }
        } catch (Exception e) {
            // 设置表读取失败按“无状态”处理
        }
        for (String uid : uids) {
            JSONObject state = stateByUid.get(uid);
            // 只统计有有效状态的机器人（state 字段非空），无状态的 uid 不展示
            if (state == null || TextUtils.isEmpty(state.optString("state"))) {
                continue;
            }
            result.add(new RobotState(uid, state, metricsByUid.get(uid)));
        }
        Collections.sort(result, (a, b) -> a.uid.compareTo(b.uid));
        return result;
    }

    /**
     * 读取指定机器人的 type=3 面板数据；robotUid 为空时取会话默认（首个匹配项）。
     * <p>
     * 多机器人会话按完整 uid 精确匹配键 {@code <convType>-<line>-<target>_3_<robotUid>}，
     * 不命中/非法返回 null。
     * </p>
     */
    public static JSONObject getAgentPanelDataFor(Conversation conversation, String robotUid) {
        if (conversation == null || TextUtils.isEmpty(conversation.target)) {
            return null;
        }
        String prefix = agentSettingKeyPrefix(conversation, Agent_PANEL_TYPE);
        try {
            if (!TextUtils.isEmpty(robotUid)) {
                // 精确匹配：单键查询，避免全量读取再筛选
                String raw = ChatManager.Instance().getUserSetting(UserSettingScope.Conversation_User_Setting, prefix + robotUid);
                return parseSettingJson(raw);
            }
            // 默认：取首个命中前缀的面板数据（兼容旧单机器人键）
            Map<String, String> settings = ChatManager.Instance().getUserSettingsLike(UserSettingScope.Conversation_User_Setting, prefix);
            if (settings != null && !settings.isEmpty()) {
                for (Map.Entry<String, String> entry : settings.entrySet()) {
                    String key = entry.getKey();
                    if (key != null && key.startsWith(prefix)) {
                        return parseSettingJson(entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /** scope=31 设置值 → JSONObject；空/非法返回 null。 */
    private static JSONObject parseSettingJson(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * 机器人“简短状态词”（多机器人标题/状态逐列展示用，参考 PC 端 robotStatusRows 的
     * textMap）：running→运行中 / waiting_user→等待确认 / thinking→思考中 /
     * done、idle 及其他→空闲；reason=error→错误。
     */
    public static String robotStateText(JSONObject state) {
        if (state == null) {
            return "空闲";
        }
        if ("error".equals(state.optString("reason"))) {
            return "错误";
        }
        switch (state.optString("state")) {
            case STATE_RUNNING:
                return "运行中";
            case STATE_WAITING_USER:
                return "等待确认";
            case "thinking":
                return "思考中";
            case STATE_IDLE:
            case STATE_DONE:
            default:
                return "空闲";
        }
    }
}
