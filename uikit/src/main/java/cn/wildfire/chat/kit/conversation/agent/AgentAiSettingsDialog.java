/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.agent;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.utils.AgentState;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.widget.WfcSheetDialogCompat;
import cn.wildfirechat.message.agent.AgentCommandMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetUserInfoCallback;
import cn.wildfirechat.remote.OnSettingUpdateListener;

/**
 * AI 会话设置面板（手机端 BottomSheet / 底部弹窗，宽屏自动变居中对话框，见
 * {@link WfcSheetDialogCompat}；内容区可滚动，适配小屏）。
 * <p>
 * 多机器人（多 agent）会话：面板先经调用方选好目标机器人（完整 uid 绑定到
 * {@link #robotUid}），标题显示机器人显示名（{@link AgentState#agentRobotName}），
 * type=3 面板数据按 uid 精确读取（{@link AgentState#getAgentPanelDataFor}），
 * 207 指令带目标 robotId（仅该机器人执行）。单聊 DSH 会话的 robotUid 即本人机器人 id。
 * </p>
 * <p>
 * 静默通道：所有交互不落消息流（不显示在界面上）。
 * 打开面板发 207 Agent_Command（op=query）组合查询 → 插件聚合面板数据
 * （effort / sandbox / plan）写入 scope=31 type=3（键 convType-line-target_3[_robotId]，
 * 不回复消息）→ 本面板读 type=3 渲染：推理等级为下拉、沙箱为单选、计划为开关。
 * 所有操作发 207 Agent_Command（op=set，cmd=命令文本，如 "/effort high"）；
 * 插件执行后写 type=1 状态 lastChange 并刷新 type=3，本面板监听本端已有的用户设置
 * 更新事件（{@link OnSettingUpdateListener}）重读 type=3，同时重读 type=2 计量
 * 刷新「上下文 xx%」占用进度（context.usedPct，未知显示 --，不轮询）。
 * </p>
 * <p>
 * 移动端精简：不再提供模型切换、会话切换、工作目录切换、新建会话与销毁会话
 * （cwd/dirs 207 请求、会话列表/当前 sessionId/207 sessions/switch、/reset、/destroy
 * 均已移除），type=3 中的 model/sessionId/cwd/dirs 字段安全忽略；保留推理等级、
 * 沙箱、计划、上下文占用进度与压缩上下文等控制。
 * </p>
 */
public class AgentAiSettingsDialog {

    /** 发送指令后控件禁用的时长（防连点），与 PC 端一致 */
    private static final long FLASH_MILLIS = 1500L;
    /** 查询兜底超时：type=3 未及时刷新也先渲染面板 */
    private static final long LOADING_TIMEOUT_MILLIS = 6000L;

    private final Dialog dialog;
    private final Conversation conversation;
    private final MessageViewModel messageViewModel;
    /** 目标机器人 uid（完整 uid，多机器人会话寻址；空=会话默认机器人） */
    private final String robotUid;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView loadingView;
    private TextView titleView;
    private Spinner effortSpinner;
    private RadioGroup sandboxGroup;
    private Switch planSwitch;
    private TextView planText;
    /** 上下文占用文案（「上下文 65%」/「上下文 --」）与进度条（context.usedPct，0–100） */
    private TextView contextUsageView;
    private ProgressBar contextProgress;
    private Button compactBtn;
    private TextView applyingView;

    /** type=3 面板数据当前值（推理等级/沙箱/计划） */
    private final List<String> effortOptions = new ArrayList<>();
    private String currentEffort = "";
    private String currentSandbox = "";
    private boolean planOn = false;
    /** 下拉实际展示项（= 候选 + 当前值不在候选时前置追加），与 Spinner 位置一一对应 */
    private final List<String> displayEffortOptions = new ArrayList<>();

    /** 程序化重建下拉/单选时的防回环开关 */
    private boolean rendering = false;
    private boolean applying = false;
    private boolean destroyed = false;
    /** 207 seq 递增序号（防重复/幂等） */
    private long commandSeq = 0;

    /** 设置更新事件：插件执行更新/查询后写 type=3/type=2，重读刷新（show 注册、dismiss 移除） */
    private final OnSettingUpdateListener settingUpdateListener = this::refreshPanelData;

    private final Runnable loadingTimeoutRunnable = () -> {
        if (!destroyed) {
            loadingView.setVisibility(View.GONE);
        }
    };
    private final Runnable flashRunnable = () -> {
        applying = false;
        updateEnabledState();
        applyingView.setVisibility(View.GONE);
    };

    public AgentAiSettingsDialog(Context context, Conversation conversation, MessageViewModel messageViewModel) {
        this(context, conversation, messageViewModel, null);
    }

    public AgentAiSettingsDialog(Context context, Conversation conversation, MessageViewModel messageViewModel, String robotUid) {
        this.dialog = WfcSheetDialogCompat.create(context);
        this.conversation = conversation;
        this.messageViewModel = messageViewModel;
        this.robotUid = robotUid;
        View view = LayoutInflater.from(context).inflate(R.layout.agent_ai_settings_dialog, null);
        dialog.setContentView(view);
        bindViews(view);
        dialog.setOnDismissListener(d -> destroy());
    }

    public void show() {
        dialog.show();
        // 监听设置更新事件：插件写 type=3（query 结果 / set 后刷新）触发重读
        ChatManager.Instance().addSettingUpdateListener(settingUpdateListener);
        // 先读已有 type=3 面板数据（若有）渲染，再发 207 query 组合查询刷新
        refreshPanelData();
        loadingView.setVisibility(View.VISIBLE);
        sendCommand("query", null);
        // 兜底：type=3 未及时刷新也先渲染面板
        handler.postDelayed(loadingTimeoutRunnable, LOADING_TIMEOUT_MILLIS);
    }

    private void bindViews(View view) {
        loadingView = view.findViewById(R.id.agentAiLoading);
        titleView = view.findViewById(R.id.agentAiTitle);
        effortSpinner = view.findViewById(R.id.agentAiEffortSpinner);
        sandboxGroup = view.findViewById(R.id.agentAiSandboxGroup);
        planSwitch = view.findViewById(R.id.agentAiPlanSwitch);
        planText = view.findViewById(R.id.agentAiPlanText);
        contextUsageView = view.findViewById(R.id.agentAiContextUsage);
        contextProgress = view.findViewById(R.id.agentAiContextProgress);
        compactBtn = view.findViewById(R.id.agentAiCompact);
        applyingView = view.findViewById(R.id.agentAiApplying);

        TextView closeView = view.findViewById(R.id.agentAiClose);
        closeView.setText("×");
        closeView.setOnClickListener(v -> dialog.dismiss());

        // 多机器人：标题显示目标机器人的显示名（用户信息缺失时回退完整 uid）
        if (!TextUtils.isEmpty(robotUid)) {
            updateTitle(AgentState.agentRobotName(robotUid));
            refreshRobotTitleAsync();
        }

        effortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (rendering || applying || position < 0 || position >= displayEffortOptions.size()) {
                    return;
                }
                String value = displayEffortOptions.get(position);
                if (value == null || value.equals(currentEffort)) {
                    return;
                }
                currentEffort = value;
                sendCommand("set", "/effort " + value);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        sandboxGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (rendering || applying || checkedId == View.NO_ID) {
                return;
            }
            RadioButton radioButton = group.findViewById(checkedId);
            if (radioButton == null || radioButton.getTag() == null) {
                return;
            }
            String value = radioButton.getTag().toString();
            if (value.equals(currentSandbox)) {
                return;
            }
            currentSandbox = value;
            sendCommand("set", "/sandbox " + value);
        });

        // Switch 的点击只来自用户操作（程序化 setChecked 不触发），天然防回环
        planSwitch.setOnClickListener(v -> {
            if (applying) {
                return;
            }
            planOn = planSwitch.isChecked();
            renderPlan();
            sendCommand("set", "/plan " + (planOn ? "on" : "off"));
        });

        compactBtn.setOnClickListener(v -> confirmAndSend(R.string.agent_ai_compact_confirm, "/compact"));
    }

    /**
     * 重读 scope=31 type=3 面板数据与 type=2 计量并渲染（设置更新事件驱动；
     * 插件执行 query 组合查询 / set 更新后写 type=3、回合结束推 type=2 触发）。
     * 多机器人会话按目标机器人 uid 精确读取（{@link AgentState#getAgentPanelDataFor} /
     * {@link AgentState#getAgentMetricsFor}）。
     */
    private void refreshPanelData() {
        if (destroyed || conversation == null) {
            return;
        }
        try {
            JSONObject data = AgentState.getAgentPanelDataFor(conversation, robotUid);
            if (data != null) {
                applyPanelData(data);
            }
        } catch (Exception ignored) {
            // 读取失败保持旧数据，下次设置更新会重读
        }
        refreshContextUsage();
    }

    /** 面板标题：绑定机器人时显示「🤖 机器人名 · AI 会话设置」；未绑定时用布局默认标题。 */
    private void updateTitle(String robotName) {
        if (titleView == null) {
            return;
        }
        if (TextUtils.isEmpty(robotName)) {
            titleView.setText(R.string.agent_ai_title);
        } else {
            titleView.setText(dialog.getContext().getString(R.string.agent_ai_title_robot, robotName));
        }
    }

    /**
     * 后台刷新目标机器人用户信息：本地未缓存时标题先显示完整 uid，拉回显示名后回填标题。
     */
    private void refreshRobotTitleAsync() {
        if (TextUtils.isEmpty(robotUid)) {
            return;
        }
        try {
            ChatManager.Instance().getUserInfo(robotUid, true, new GetUserInfoCallback() {
                @Override
                public void onSuccess(UserInfo userInfo) {
                    if (destroyed || userInfo == null) {
                        return;
                    }
                    String name = !TextUtils.isEmpty(userInfo.displayName) ? userInfo.displayName
                        : !TextUtils.isEmpty(userInfo.name) ? userInfo.name : "";
                    if (!TextUtils.isEmpty(name)) {
                        handler.post(() -> updateTitle(name));
                    }
                }

                @Override
                public void onFail(int errorCode) {
                    // 拉取失败保持完整 uid 回退展示
                }
            });
        } catch (Exception ignored) {
            // 用户信息刷新失败不影响面板
        }
    }

    /**
     * 把 type=3 面板数据应用到当前值并渲染：effort.options/current、sandbox.current、plan.on。
     * model / sessionId / cwd / dirs 等移动端已移除的字段直接忽略。
     */
    private void applyPanelData(JSONObject data) {
        // 推理等级：current + options（字符串数组）
        JSONObject effort = data.optJSONObject("effort");
        if (effort != null) {
            currentEffort = effort.optString("current", "");
            effortOptions.clear();
            JSONArray options = effort.optJSONArray("options");
            if (options != null) {
                for (int i = 0; i < options.length(); i++) {
                    String value = options.optString(i);
                    if (!TextUtils.isEmpty(value) && !effortOptions.contains(value)) {
                        effortOptions.add(value);
                    }
                }
            }
        }
        // 沙箱：current
        JSONObject sandbox = data.optJSONObject("sandbox");
        if (sandbox != null) {
            currentSandbox = sandbox.optString("current", "");
        }
        // 计划：on
        JSONObject plan = data.optJSONObject("plan");
        if (plan != null) {
            planOn = plan.optBoolean("on", false);
        }

        renderEffort();
        renderSandbox();
        renderPlan();
        // type=3 就绪：隐藏加载中提示
        loadingView.setVisibility(View.GONE);
        handler.removeCallbacks(loadingTimeoutRunnable);
    }

    /**
     * 读 scope=31 type=2 计量并刷新上下文占用进度（context.usedPct）。
     * 未知/缺失/非法时显示「上下文 --」+ 空进度条；不轮询，仅面板打开与设置更新事件驱动。
     */
    private void refreshContextUsage() {
        Double usedPct = null;
        try {
            JSONObject metrics = AgentState.getAgentMetricsFor(conversation, robotUid);
            JSONObject context = metrics == null ? null : metrics.optJSONObject("context");
            if (context != null) {
                Object value = context.opt("usedPct");
                if (value instanceof Number) {
                    usedPct = ((Number) value).doubleValue();
                }
            }
        } catch (Exception ignored) {
            // 读取失败按未知处理
        }
        renderContextUsage(usedPct);
    }

    /** 渲染上下文占用：文案「上下文 65%」/「上下文 --」，进度 0–100 钳制，未知时进度为 0 */
    private void renderContextUsage(Double usedPct) {
        if (contextUsageView == null || contextProgress == null) {
            return;
        }
        if (usedPct == null || usedPct.isNaN() || usedPct.isInfinite()) {
            contextUsageView.setText(R.string.agent_ai_context_unknown);
            contextProgress.setProgress(0);
            return;
        }
        double pct = Math.max(0d, Math.min(100d, usedPct));
        contextProgress.setProgress((int) Math.round(pct));
        contextUsageView.setText(dialog.getContext().getString(
            R.string.agent_ai_context_usage, formatPercent(pct) + "%"));
    }

    /** 百分比数字：整数不带小数，小数保留 1 位（如 65、70.5） */
    private String formatPercent(double pct) {
        if (pct == Math.rint(pct)) {
            return String.valueOf((long) pct);
        }
        return String.valueOf(Math.round(pct * 10) / 10.0);
    }

    /**
     * 发送 207 Agent_Command 面板指令（透明消息，不显示在消息流）。
     * op=query 组合查询（cmd 空）；op=set 更新（cmd=命令文本，如 "/effort high"）。
     * 绑定目标机器人时 207 带 robotId（完整 uid）：多机器人会话仅该机器人执行
     * （服务端插件已支持 robotId 寻址）。
     * set 发送后控件短暂禁用（防连点）。
     */
    private void sendCommand(String op, String cmd) {
        if (destroyed || conversation == null || messageViewModel == null) {
            return;
        }
        AgentCommandMessageContent content = new AgentCommandMessageContent(op, cmd, nextSeq(), robotUid);
        messageViewModel.sendMessage(conversation, content);
        if ("set".equals(op)) {
            applying = true;
            updateEnabledState();
            applyingView.setVisibility(View.VISIBLE);
            handler.removeCallbacks(flashRunnable);
            handler.postDelayed(flashRunnable, FLASH_MILLIS);
        }
    }

    /** 207 seq 自增（同一面板会话内单调递增，用于幂等） */
    private long nextSeq() {
        return ++commandSeq;
    }

    /** 重建推理等级下拉：候选来自 type=3 effort.options；当前值不在候选里也追加显示并选中 */
    private void renderEffort() {
        rendering = true;
        displayEffortOptions.clear();
        displayEffortOptions.addAll(effortOptions);
        if (!displayEffortOptions.contains(currentEffort) && !TextUtils.isEmpty(currentEffort)) {
            displayEffortOptions.add(0, currentEffort);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(dialog.getContext(),
            android.R.layout.simple_spinner_item, displayEffortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        effortSpinner.setAdapter(adapter);
        int index = displayEffortOptions.indexOf(currentEffort);
        effortSpinner.setSelection(Math.max(0, index));
        handler.post(() -> rendering = false);
    }

    /** 沙箱模式固定三项单选，按当前值高亮 */
    private void renderSandbox() {
        rendering = true;
        sandboxGroup.clearCheck();
        if (!TextUtils.isEmpty(currentSandbox)) {
            for (int i = 0; i < sandboxGroup.getChildCount(); i++) {
                View child = sandboxGroup.getChildAt(i);
                if (child instanceof RadioButton && currentSandbox.equals(child.getTag())) {
                    sandboxGroup.check(child.getId());
                    break;
                }
            }
        }
        rendering = false;
    }

    private void renderPlan() {
        planSwitch.setChecked(planOn);
        planText.setText(planOn ? R.string.agent_ai_plan_on : R.string.agent_ai_plan_off);
    }

    private void confirmAndSend(int contentRes, String command) {
        new MaterialDialog.Builder(dialog.getContext())
            .content(contentRes)
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .onPositive((dialog1, which) -> sendCommand("set", command))
            .show();
    }

    private void updateEnabledState() {
        boolean enabled = !applying;
        effortSpinner.setEnabled(enabled);
        setRadioGroupEnabled(sandboxGroup, enabled);
        planSwitch.setEnabled(enabled);
        compactBtn.setEnabled(enabled);
    }

    private void setRadioGroupEnabled(RadioGroup group, boolean enabled) {
        if (group == null) {
            return;
        }
        group.setEnabled(enabled);
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
    }

    private void destroy() {
        destroyed = true;
        ChatManager.Instance().removeSettingUpdateListener(settingUpdateListener);
        handler.removeCallbacksAndMessages(null);
    }
}
