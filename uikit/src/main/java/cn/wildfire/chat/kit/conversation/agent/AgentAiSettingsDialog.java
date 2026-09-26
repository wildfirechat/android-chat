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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.utils.AgentState;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.widget.WfcSheetDialogCompat;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.agent.AgentCommandMessageContent;
import cn.wildfirechat.message.agent.AgentCommandResultMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetUserInfoCallback;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.OnSettingUpdateListener;

/**
 * AI 会话设置面板（手机端 BottomSheet / 底部弹窗，宽屏自动变居中对话框，见
 * {@link WfcSheetDialogCompat}；内容区可滚动，适配小屏）。
 * <p>
 * 多机器人（多 agent）会话：面板先经调用方选好目标机器人（完整 uid 绑定到
 * {@link #robotUid}），标题显示机器人显示名（{@link AgentState#agentRobotName}），
 * type=3 面板数据按 uid 精确读取（{@link AgentState#getAgentPanelDataFor}），
 * 207 指令带目标 robotId（仅该机器人执行）。
 * </p>
 * <p>
 * 静默通道：所有交互不落消息流（不显示在界面上）。
 * 打开面板发 207 Agent_Command（op=query）组合查询 → 插件聚合面板数据
 * （model 当前值+目录 / effort / sandbox / plan / cwd / sessionId / preset / approval / mode）写入
 * scope=31 type=3（键 convType-line-target_3[_robotId]，不回复消息）→ 本面板读 type=3 渲染：
 * 模型/推理等级/Agent 模式（preset）/工具审批（approval）/会话模式（mode）为下拉（options + current）、
 * 沙箱为单选、计划为开关、工作目录为 cwd + 目录选择弹窗。
 * preset.options / approval.options 为空（未部署 preset 服务 / 旧插件缺字段）时对应下拉禁用
 * 但保留 current 文本展示；current 不在 options 中时置顶追加展示，不丢当前值。
 * 会话模式（mode，interrupt/queue）与二者不同：切换只依赖本地状态，插件缺 mode 字段
 * （旧插件）时用本地兜底两项，故候选永不为空、控件始终可切换；mode.current 存在时同步本地
 * 状态并据此回显选中项（修复「切到 queue 后重开面板仍显示 interrupt」）。
 * 所有操作发 207 Agent_Command（op=set，cmd=命令文本，如 "/model deepseek-official/xxx"）；
 * 插件执行后写 type=1 状态 lastChange（如 "模型 → deepseek-official/deepseek-v4-pro"，变更可见）
 * 并刷新 type=3，本面板监听本端已有的用户设置更新事件（{@link OnSettingUpdateListener}）重读 type=3。
 * 不再发送 /model /effort /sandbox /plan /ls 等文本命令、不再解析机器人回复文本
 * （parseModelReply / parseEffortReply / parseSandboxReply / parsePlanReply / parseLsReply 已移除）。
 * 207 为透明消息（PersistFlag.Transparent，digest 空）：不持久化、不显示。
 * </p>
 * <p>
 * 目录列表按需获取（v2.3）：插件已把 {@code dirs} 从 type=3 移除（避免 scope=31 单值 4096 字符超限），
 * 改为用户点「切换」时按需请求——发 207 {@code op=dirs}（带 seq/robotId）并显示加载态，
 * 插件用 209 {@link AgentCommandResultMessageContent} 透明消息回传，本面板按 seq 关联 pending、
 * 校验 robotId 后渲染候选（TTL 60s 缓存）。超时 5s 重试 1 次，仍失败提示
 * 「获取目录失败，请重试」并保留手动输入路径兜底；老插件（type=3 仍带 dirs）直接使用其 dirs，
 * 无 209 应答时回退读 type=3 的 dirs。应答监听随 dialog dismiss 解绑。
 * </p>
 */
public class AgentAiSettingsDialog {

    /** 发送指令后控件禁用的时长（防连点），与 PC 端一致 */
    private static final long FLASH_MILLIS = 1500L;
    /** 查询兜底超时：type=3 未及时刷新也先渲染面板 */
    private static final long LOADING_TIMEOUT_MILLIS = 6000L;
    /** 目录列表按需请求（207 op=dirs）单次等待 209 应答的超时 */
    private static final long DIRS_REQUEST_TIMEOUT_MILLIS = 5000L;
    /** 目录列表按需请求超时后的最大重试次数（5s 超时 + 重试 1 次） */
    private static final int DIRS_REQUEST_MAX_RETRY = 1;
    /** 目录列表缓存有效期（同一面板会话内 TTL 60s，避免重复请求） */
    private static final long DIRS_CACHE_TTL_MILLIS = 60000L;
    /** 207 op=dirs（目录列表按需获取） */
    private static final String OP_DIRS = "dirs";

    /** 待应答的 207 op=dirs 请求（按 seq 关联；应答匹配/超时后移除） */
    private static class DirsRequest {
        final long seq;
        /** 请求目标机器人 uid（空=会话默认机器人） */
        final String robotId;
        /** 已重试次数 */
        int retry;

        DirsRequest(long seq, String robotId) {
            this.seq = seq;
            this.robotId = robotId;
        }
    }

    /** 面板候选项（模型 / Agent 模式 / 工具审批 / 会话模式，options 同构）：value=发送用，label=展示用 */
    private static class ModelOption {
        final String value;
        final String label;

        ModelOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }

    private final Dialog dialog;
    private final Conversation conversation;
    private final MessageViewModel messageViewModel;
    /** 目标机器人 uid（完整 uid，多机器人会话寻址；空=会话默认机器人） */
    private final String robotUid;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView loadingView;
    private TextView titleView;
    private Spinner modelSpinner;
    private Spinner effortSpinner;
    private Spinner presetSpinner;
    private Spinner approvalSpinner;
    /** 会话模式（mode，interrupt/queue）：与 preset/approval 同为 {value,label} 下拉 */
    private Spinner modeSpinner;
    private RadioGroup sandboxGroup;
    private TextView cwdCurrentView;
    private TextView cwdListLoadingView;
    private LinearLayout cwdListContainer;
    private TextView cwdListEmptyView;
    private Button cwdSwitchBtn;
    private Button cwdManualBtn;
    private Switch planSwitch;
    private TextView planText;
    private Button compactBtn;
    private Button resetBtn;
    private Button destroyBtn;
    private TextView applyingView;

    /** type=3 面板数据当前值（模型/推理等级/沙箱/计划/工作目录/Agent 模式/工具审批/会话模式） */
    private final List<ModelOption> modelOptions = new ArrayList<>();
    private String currentModel = "";
    private final List<String> effortOptions = new ArrayList<>();
    private String currentEffort = "";
    /** Agent 模式（preset）：候选可能为空数组（未部署 preset 服务）→ 下拉禁用但显示 current */
    private final List<ModelOption> presetOptions = new ArrayList<>();
    private String currentPreset = "";
    /** 工具审批（approval）：旧插件可能整体缺字段 → 下拉禁用但显示 current */
    private final List<ModelOption> approvalOptions = new ArrayList<>();
    private String currentApproval = "";
    /**
     * 会话模式（mode）：候选来自 type=3 mode.options；插件缺该字段（旧插件）时用本地兜底两项
     * （见 {@link #localModeOptions()}），故候选永不为空、控件始终可切换。
     */
    private final List<ModelOption> modeOptions = new ArrayList<>();
    /** 会话模式当前值：本地默认 interrupt；type=3 mode.current 存在时同步覆盖（服务端回显） */
    private String currentMode = "interrupt";
    private String currentSandbox = "";
    private boolean planOn = false;
    private String currentCwd = "";
    /** 目录候选（渲染用）：209 应答缓存优先，否则用老插件 type=3 的 dirs */
    private final List<String> cwdCandidates = new ArrayList<>();
    /** 209 Agent_Command_Result 应答的目录候选缓存（TTL {@link #DIRS_CACHE_TTL_MILLIS}） */
    private final List<String> dirsCache = new ArrayList<>();
    /** 缓存写入时刻（System.currentTimeMillis，0=无缓存） */
    private long dirsCacheAt = 0L;
    /** 应答中的根目录（仅展示/兜底用，可能为空） */
    private String dirsRoot = "";
    /** 应答中的总条数（0=未提供） */
    private int dirsTotal = 0;
    /** 应答是否被插件截断 */
    private boolean dirsTruncated = false;
    /** 老插件兼容：type=3 仍携带的 dirs（新版插件已移除该字段，可能为空） */
    private final List<String> legacyDirs = new ArrayList<>();
    /** 待应答的 207 op=dirs 请求（seq → 请求），应答按 seq 关联 */
    private final Map<Long, DirsRequest> pendingDirsRequests = new LinkedHashMap<>();
    /** 下拉实际展示项（= 候选 + 当前值不在候选时前置追加），与 Spinner 位置一一对应 */
    private final List<ModelOption> displayModelOptions = new ArrayList<>();
    private final List<String> displayEffortOptions = new ArrayList<>();
    private final List<ModelOption> displayPresetOptions = new ArrayList<>();
    private final List<ModelOption> displayApprovalOptions = new ArrayList<>();
    private final List<ModelOption> displayModeOptions = new ArrayList<>();

    /** 程序化重建下拉/单选时的防回环开关 */
    private boolean rendering = false;
    private boolean applying = false;
    private boolean destroyed = false;
    /** 目录列表是否已打开（未打开时收到 type=3 刷新只更新候选，不展示） */
    private boolean cwdListOpen = false;
    /** 目录列表是否处于加载态（207 op=dirs 已发出、209 应答未到） */
    private boolean dirsLoading = false;
    /** 207 seq 递增序号（防重复/幂等；op=dirs 的 seq 用于关联 209 应答） */
    private long commandSeq = 0;

    /** 设置更新事件：插件执行更新/查询后写 type=3，重读刷新（show 注册、dismiss 移除） */
    private final OnSettingUpdateListener settingUpdateListener = this::refreshPanelData;

    /** 消息接收事件：209 Agent_Command_Result（透明消息）按 seq 关联 pending dirs 请求（show 注册、dismiss 移除） */
    private final OnReceiveMessageListener receiveMessageListener = this::onReceiveMessages;

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
    /** 目录列表按需请求超时：先重试 1 次（复用同一 seq），仍失败则回退/提示 */
    private final Runnable dirsRequestTimeoutRunnable = this::onDirsRequestTimeout;

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
        // 监听消息接收事件：209 Agent_Command_Result（透明消息）回传目录列表，按 seq 关联 pending
        ChatManager.Instance().addOnReceiveMessageListener(receiveMessageListener);
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
        modelSpinner = view.findViewById(R.id.agentAiModelSpinner);
        effortSpinner = view.findViewById(R.id.agentAiEffortSpinner);
        presetSpinner = view.findViewById(R.id.agentAiPresetSpinner);
        approvalSpinner = view.findViewById(R.id.agentAiApprovalSpinner);
        modeSpinner = view.findViewById(R.id.agentAiModeSpinner);
        // 面板数据到达前（无候选）先禁用：避免空下拉可点；applyPanelData 渲染时按候选可用性恢复
        presetSpinner.setEnabled(false);
        approvalSpinner.setEnabled(false);
        sandboxGroup = view.findViewById(R.id.agentAiSandboxGroup);
        cwdCurrentView = view.findViewById(R.id.agentAiCwdCurrent);
        cwdListLoadingView = view.findViewById(R.id.agentAiCwdListLoading);
        cwdListContainer = view.findViewById(R.id.agentAiCwdList);
        cwdListEmptyView = view.findViewById(R.id.agentAiCwdListEmpty);
        cwdSwitchBtn = view.findViewById(R.id.agentAiCwdSwitch);
        cwdManualBtn = view.findViewById(R.id.agentAiCwdManual);
        planSwitch = view.findViewById(R.id.agentAiPlanSwitch);
        planText = view.findViewById(R.id.agentAiPlanText);
        compactBtn = view.findViewById(R.id.agentAiCompact);
        resetBtn = view.findViewById(R.id.agentAiReset);
        destroyBtn = view.findViewById(R.id.agentAiDestroy);
        applyingView = view.findViewById(R.id.agentAiApplying);

        TextView closeView = view.findViewById(R.id.agentAiClose);
        closeView.setText("×");
        closeView.setOnClickListener(v -> dialog.dismiss());

        // 多机器人：标题显示目标机器人的显示名（用户信息缺失时回退完整 uid）
        if (!TextUtils.isEmpty(robotUid)) {
            updateTitle(AgentState.agentRobotName(robotUid));
            refreshRobotTitleAsync();
        }

        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (rendering || applying || position < 0 || position >= displayModelOptions.size()) {
                    return;
                }
                ModelOption option = displayModelOptions.get(position);
                if (option == null || option.value.equals(currentModel)) {
                    return;
                }
                currentModel = option.value;
                sendCommand("set", "/model " + option.value);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (rendering || applying || position < 0 || position >= displayPresetOptions.size()) {
                    return;
                }
                ModelOption option = displayPresetOptions.get(position);
                if (option == null || option.value.equals(currentPreset)) {
                    return;
                }
                currentPreset = option.value;
                sendCommand("set", "/preset " + option.value);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        approvalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (rendering || applying || position < 0 || position >= displayApprovalOptions.size()) {
                    return;
                }
                ModelOption option = displayApprovalOptions.get(position);
                if (option == null || option.value.equals(currentApproval)) {
                    return;
                }
                currentApproval = option.value;
                sendCommand("set", "/approval " + option.value);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (rendering || applying || position < 0 || position >= displayModeOptions.size()) {
                    return;
                }
                ModelOption option = displayModeOptions.get(position);
                if (option == null || option.value.equals(currentMode)) {
                    return;
                }
                currentMode = option.value;
                sendCommand("set", "/mode " + option.value);
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

        cwdSwitchBtn.setOnClickListener(v -> {
            if (applying) {
                return;
            }
            openCwdList();
        });
        // 手动输入兜底：获取目录失败时仍可直接输入绝对路径（发 207 set /cwd <路径>）
        cwdManualBtn.setOnClickListener(v -> {
            if (applying) {
                return;
            }
            promptManualCwd();
        });

        compactBtn.setOnClickListener(v -> confirmAndSend(R.string.agent_ai_compact_confirm, "/compact"));
        resetBtn.setOnClickListener(v -> confirmAndSend(R.string.agent_ai_reset_confirm, "/reset"));
        // 销毁会话：毁灭性操作，不受操作冷却禁用，始终可点（点击弹强警告，确认后才发送）
        destroyBtn.setOnClickListener(v -> confirmAndSend(R.string.agent_ai_destroy_confirm, "/destroy"));

        // 会话模式：面板数据到达前先用本地兜底两项（interrupt/queue）渲染；
        // 插件缺 mode 字段（旧插件）时保持兜底，控件始终可切换
        modeOptions.addAll(localModeOptions());
        renderMode();
    }

    /**
     * 重读 scope=31 type=3 面板数据并渲染（设置更新事件驱动；
     * 插件执行 query 组合查询 / set 更新后写 type=3 触发）。
     * 多机器人会话按目标机器人 uid 精确读取（{@link AgentState#getAgentPanelDataFor}）。
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
     * 把 type=3 面板数据应用到当前值并渲染：
     * model.options/current、effort.options/current、preset.options/current、
     * approval.options/current、sandbox.current、plan.on、cwd；
     * {@code dirs} 已由插件移除（改走 209 按需获取），仅当老插件仍携带时直接使用（兼容）。
     */
    private void applyPanelData(JSONObject data) {
        // 模型：current + options（value=provider/id，label=value（名））
        JSONObject model = data.optJSONObject("model");
        if (model != null) {
            currentModel = model.optString("current", "");
            modelOptions.clear();
            JSONArray options = model.optJSONArray("options");
            if (options != null) {
                for (int i = 0; i < options.length(); i++) {
                    JSONObject option = options.optJSONObject(i);
                    if (option == null) {
                        continue;
                    }
                    String value = option.optString("value");
                    if (TextUtils.isEmpty(value)) {
                        continue;
                    }
                    String label = option.optString("label");
                    if (TextUtils.isEmpty(label)) {
                        label = value;
                    }
                    modelOptions.add(new ModelOption(value, label));
                }
            }
        }
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
        // Agent 模式（preset）：current + options（与 model.options 同构；options 可能为空数组）
        currentPreset = parseSelectorCurrent(data, "preset");
        presetOptions.clear();
        presetOptions.addAll(parseSelectorOptions(data, "preset"));
        // 工具审批（approval）：current + options（旧插件可能整体缺字段 → 空列表 + 空 current）
        currentApproval = parseSelectorCurrent(data, "approval");
        approvalOptions.clear();
        approvalOptions.addAll(parseSelectorOptions(data, "approval"));
        // 会话模式（mode）：current 存在时同步本地状态（服务端回显，修复重开面板回默认值）；
        // options 为空（旧插件缺字段）时保留本地兜底两项
        JSONObject mode = data.optJSONObject("mode");
        if (mode != null) {
            String currentModeValue = mode.optString("current", "");
            if (!TextUtils.isEmpty(currentModeValue)) {
                currentMode = currentModeValue;
            }
            List<ModelOption> parsedModeOptions = parseSelectorOptions(data, "mode");
            if (!parsedModeOptions.isEmpty()) {
                modeOptions.clear();
                modeOptions.addAll(parsedModeOptions);
            }
        }
        // 计划：on
        JSONObject plan = data.optJSONObject("plan");
        if (plan != null) {
            planOn = plan.optBoolean("on", false);
        }
        // 工作目录 + 根目录子目录（dirs 新版插件已移除，改走 207 op=dirs → 209 应答）
        currentCwd = data.optString("cwd", "");
        legacyDirs.clear();
        JSONArray dirs = data.optJSONArray("dirs");
        if (dirs != null) {
            for (int i = 0; i < dirs.length(); i++) {
                String dir = dirs.optString(i);
                if (!TextUtils.isEmpty(dir)) {
                    legacyDirs.add(dir);
                }
            }
            if (!legacyDirs.isEmpty()) {
                // 老插件兼容：type=3 仍带 dirs，直接作为缓存使用（TTL 内点“切换”不再发 207）
                dirsCache.clear();
                dirsCache.addAll(legacyDirs);
                dirsCacheAt = System.currentTimeMillis();
                dirsTotal = legacyDirs.size();
                dirsTruncated = false;
                String legacyRoot = data.optString("root", "");
                if (!TextUtils.isEmpty(legacyRoot)) {
                    dirsRoot = legacyRoot;
                }
            }
        }
        updateCwdCandidates();

        renderModel();
        renderEffort();
        renderPreset();
        renderApproval();
        renderMode();
        renderSandbox();
        renderPlan();
        renderCwd();
        // type=3 就绪：隐藏加载中提示
        loadingView.setVisibility(View.GONE);
        handler.removeCallbacks(loadingTimeoutRunnable);
    }

    /**
     * 读面板数据中 {current, options:[{value,label}]} 字段的 current（preset/approval）。
     * 字段整体缺失/非对象（旧插件）返回空串：控件显示占位并禁用，不报错、不发请求。
     */
    private String parseSelectorCurrent(JSONObject data, String key) {
        JSONObject selector = data.optJSONObject(key);
        return selector != null ? selector.optString("current", "") : "";
    }

    /**
     * 解析面板数据中 {current, options:[{value,label}]} 字段的候选（preset/approval，
     * 与 model.options 同构）；字段缺失/options 非数组返回空列表（控件禁用）；
     * 非法项（非对象 / value 为空）跳过，label 缺省回退 value。
     */
    private List<ModelOption> parseSelectorOptions(JSONObject data, String key) {
        List<ModelOption> result = new ArrayList<>();
        JSONObject selector = data.optJSONObject(key);
        if (selector == null) {
            return result;
        }
        JSONArray options = selector.optJSONArray("options");
        if (options == null) {
            return result;
        }
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option == null) {
                continue;
            }
            String value = option.optString("value");
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            String label = option.optString("label");
            if (TextUtils.isEmpty(label)) {
                label = value;
            }
            result.add(new ModelOption(value, label));
        }
        return result;
    }

    /**
     * 发送 207 Agent_Command 面板指令（透明消息，不显示在消息流）。
     * op=query 组合查询（cmd 空）；op=set 更新（cmd=命令文本，如 "/model deepseek-official/xxx"）；
     * op=dirs 目录列表按需获取（应答走 209，见 {@link #requestDirs()}）。
     * 绑定目标机器人时 207 带 robotId（完整 uid）：多机器人会话仅该机器人执行
     * （服务端插件已支持 robotId 寻址）。
     * set 发送后控件短暂禁用（防连点）。
     */
    private void sendCommand(String op, String cmd) {
        sendCommandWithSeq(op, cmd, ++commandSeq);
    }

    /** 按指定 seq 发送 207（op=dirs 需要把 seq 记入 pending，用于关联 209 应答） */
    private void sendCommandWithSeq(String op, String cmd, long seq) {
        if (destroyed || conversation == null || messageViewModel == null) {
            return;
        }
        AgentCommandMessageContent content = new AgentCommandMessageContent(op, cmd, seq, robotUid);
        messageViewModel.sendMessage(conversation, content);
        if ("set".equals(op)) {
            applying = true;
            updateEnabledState();
            applyingView.setVisibility(View.VISIBLE);
            handler.removeCallbacks(flashRunnable);
            handler.postDelayed(flashRunnable, FLASH_MILLIS);
        }
    }

    /**
     * 消息接收回调：只处理 209 {@link AgentCommandResultMessageContent}（透明消息，不显示）。
     * 按会话过滤后交给 {@link #handleCommandResult}，seq 不匹配/超时的应答直接丢弃。
     */
    private void onReceiveMessages(List<Message> messages, boolean hasMore) {
        if (destroyed || messages == null || messages.isEmpty()) {
            return;
        }
        for (Message message : messages) {
            if (message == null || !(message.content instanceof AgentCommandResultMessageContent)) {
                continue;
            }
            if (conversation != null && !conversation.equals(message.conversation)) {
                // 其它会话的应答忽略（本面板只关心本会话的目录请求）
                continue;
            }
            handleCommandResult((AgentCommandResultMessageContent) message.content);
        }
    }

    /**
     * 处理 209 Agent_Command_Result：op=dirs 且 seq 命中 pending 才接受；
     * 请求指定了机器人时校验 robotId（应答 robotId 为空视为兼容接受，seq 已唯一）。
     * 未匹配（seq 未知/已超时移除）或 robotId 不符的应答直接丢弃。
     */
    private void handleCommandResult(AgentCommandResultMessageContent content) {
        if (!OP_DIRS.equals(content.getOp())) {
            // 其它 op 的应答本面板不消费
            return;
        }
        DirsRequest request = pendingDirsRequests.get(content.getSeq());
        if (request == null) {
            // seq 不匹配或已超时：丢弃
            return;
        }
        if (!TextUtils.isEmpty(request.robotId)
            && !TextUtils.isEmpty(content.getRobotId())
            && !request.robotId.equals(content.getRobotId())) {
            // 非目标机器人的应答：丢弃（保持 pending，等本机器人应答/超时）
            return;
        }
        pendingDirsRequests.remove(request.seq);
        handler.removeCallbacks(dirsRequestTimeoutRunnable);
        dirsLoading = false;

        // 缓存应答（TTL 60s）
        dirsCache.clear();
        dirsCache.addAll(content.getDirs());
        dirsCacheAt = System.currentTimeMillis();
        dirsRoot = content.getRoot() != null ? content.getRoot() : "";
        dirsTotal = content.getTotal();
        dirsTruncated = content.isTruncated();

        if (!cwdListOpen) {
            // 列表已关闭：只更新缓存，等下次打开直接用
            updateEnabledState();
            return;
        }
        updateCwdCandidates();
        cwdListLoadingView.setVisibility(View.GONE);
        if (cwdCandidates.isEmpty()) {
            cwdListContainer.setVisibility(View.GONE);
            cwdListEmptyView.setText(R.string.agent_ai_cwd_list_empty);
            cwdListEmptyView.setVisibility(View.VISIBLE);
        } else {
            renderCwdList();
            cwdListEmptyView.setVisibility(View.GONE);
            cwdListContainer.setVisibility(View.VISIBLE);
        }
        updateEnabledState();
    }

    /** 发 207 op=dirs 并登记 pending（seq 关联 209 应答），同时启动 5s 超时 */
    private void requestDirs() {
        long seq = ++commandSeq;
        pendingDirsRequests.put(seq, new DirsRequest(seq, robotUid));
        dirsLoading = true;
        sendCommandWithSeq(OP_DIRS, null, seq);
        handler.removeCallbacks(dirsRequestTimeoutRunnable);
        handler.postDelayed(dirsRequestTimeoutRunnable, DIRS_REQUEST_TIMEOUT_MILLIS);
    }

    /**
     * 207 op=dirs 超时：首次超时重试 1 次（复用同一 seq，迟到的首次应答仍可关联）；
     * 重试用尽则回退/提示。
     */
    private void onDirsRequestTimeout() {
        if (destroyed || pendingDirsRequests.isEmpty()) {
            return;
        }
        for (DirsRequest request : pendingDirsRequests.values()) {
            if (request.retry < DIRS_REQUEST_MAX_RETRY) {
                request.retry++;
                sendCommandWithSeq(OP_DIRS, null, request.seq);
                handler.removeCallbacks(dirsRequestTimeoutRunnable);
                handler.postDelayed(dirsRequestTimeoutRunnable, DIRS_REQUEST_TIMEOUT_MILLIS);
                return;
            }
        }
        pendingDirsRequests.clear();
        dirsLoading = false;
        onDirsRequestFailed();
    }

    /**
     * 目录按需获取最终失败：
     * 兼容老插件——重新读一次 type=3，若仍带 dirs（老插件不识别 op=dirs）则回退使用；
     * 否则提示「获取目录失败，请重试」，保留手动输入路径兜底。
     */
    private void onDirsRequestFailed() {
        if (legacyDirs.isEmpty()) {
            readLegacyDirsFromSetting();
        }
        cwdListLoadingView.setVisibility(View.GONE);
        if (!legacyDirs.isEmpty()) {
            // 老插件回退：用 type=3 的 dirs 渲染，并刷新缓存避免每次点击都等超时
            dirsCache.clear();
            dirsCache.addAll(legacyDirs);
            dirsCacheAt = System.currentTimeMillis();
            dirsTruncated = false;
            dirsTotal = legacyDirs.size();
            updateCwdCandidates();
            if (cwdListOpen) {
                renderCwdList();
                cwdListEmptyView.setVisibility(View.GONE);
                cwdListContainer.setVisibility(View.VISIBLE);
            }
        } else if (cwdListOpen) {
            updateCwdCandidates();
            cwdListContainer.setVisibility(View.GONE);
            cwdListEmptyView.setText(R.string.agent_ai_cwd_list_failed);
            cwdListEmptyView.setVisibility(View.VISIBLE);
        }
        updateEnabledState();
    }

    /** 老插件兼容回退：直接读一次 type=3 面板数据里的 dirs（新版插件已无该字段） */
    private void readLegacyDirsFromSetting() {
        if (conversation == null) {
            return;
        }
        try {
            JSONObject data = AgentState.getAgentPanelDataFor(conversation, robotUid);
            if (data == null) {
                return;
            }
            JSONArray dirs = data.optJSONArray("dirs");
            legacyDirs.clear();
            if (dirs != null) {
                for (int i = 0; i < dirs.length(); i++) {
                    String dir = dirs.optString(i);
                    if (!TextUtils.isEmpty(dir)) {
                        legacyDirs.add(dir);
                    }
                }
            }
        } catch (Exception ignored) {
            // 读取失败按“无 dirs”处理
        }
    }

    /** 209 缓存是否命中（同一面板会话内 TTL 60s 且非空） */
    private boolean isDirsCacheValid() {
        return !dirsCache.isEmpty()
            && dirsCacheAt > 0
            && System.currentTimeMillis() - dirsCacheAt <= DIRS_CACHE_TTL_MILLIS;
    }

    /** 重算渲染用候选：209 缓存优先，否则老插件 type=3 的 dirs */
    private void updateCwdCandidates() {
        cwdCandidates.clear();
        if (!dirsCache.isEmpty()) {
            cwdCandidates.addAll(dirsCache);
        } else {
            cwdCandidates.addAll(legacyDirs);
        }
    }

    /** 重建模型下拉：候选来自 type=3 model.options；当前值不在候选里也追加显示并选中 */
    private void renderModel() {
        rendering = true;
        displayModelOptions.clear();
        displayModelOptions.addAll(modelOptions);
        boolean hasCurrent = false;
        for (ModelOption option : displayModelOptions) {
            if (option.value.equals(currentModel)) {
                hasCurrent = true;
                break;
            }
        }
        if (!hasCurrent && !TextUtils.isEmpty(currentModel)) {
            displayModelOptions.add(0, new ModelOption(currentModel, currentModel));
        }
        List<String> labels = new ArrayList<>();
        for (ModelOption option : displayModelOptions) {
            labels.add(option.label);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(dialog.getContext(),
            android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        int index = 0;
        for (int i = 0; i < displayModelOptions.size(); i++) {
            if (displayModelOptions.get(i).value.equals(currentModel)) {
                index = i;
                break;
            }
        }
        modelSpinner.setSelection(index);
        // 下拉重建的 onItemSelected 回调在布局后异步触发，延后清除防回环开关
        handler.post(() -> rendering = false);
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

    /**
     * 重建 Agent 模式下拉：候选来自 type=3 preset.options；
     * options 为空数组（部署未提供 preset 服务）时禁用控件，仅显示 current（缺省「未设置」）。
     */
    private void renderPreset() {
        displayPresetOptions.clear();
        renderSelector(presetSpinner, presetOptions, displayPresetOptions, currentPreset);
    }

    /**
     * 重建工具审批下拉：候选来自 type=3 approval.options；
     * 字段整体缺失（旧插件）时禁用控件，仅显示 current（缺省「未设置」）。
     */
    private void renderApproval() {
        displayApprovalOptions.clear();
        renderSelector(approvalSpinner, approvalOptions, displayApprovalOptions, currentApproval);
    }

    /**
     * 重建会话模式下拉：候选优先取 type=3 mode.options，缺失/为空（旧插件）时用本地兜底两项；
     * 当前值不在候选中时置顶追加并选中，保证服务端 mode.current 始终可见（回显）。
     */
    private void renderMode() {
        displayModeOptions.clear();
        renderSelector(modeSpinner, modeOptions, displayModeOptions, currentMode);
    }

    /**
     * 会话模式本地兜底候选（interrupt/queue）：插件缺 {@code mode} 字段（旧插件）或面板数据
     * 尚未到达时使用；插件下发的 mode.options 存在时以服务端为准覆盖。
     */
    private List<ModelOption> localModeOptions() {
        List<ModelOption> defaults = new ArrayList<>();
        defaults.add(new ModelOption("interrupt",
            dialog.getContext().getString(R.string.agent_ai_conv_mode_interrupt)));
        defaults.add(new ModelOption("queue",
            dialog.getContext().getString(R.string.agent_ai_conv_mode_queue)));
        return defaults;
    }

    /**
     * 重建 {value,label} 候选下拉（Agent 模式 / 工具审批 / 会话模式，与 model.options 同构）：
     * 展示项 = 候选 + 当前值（不在候选中时置顶追加，保证 current 始终可见并被选中）；
     * 候选为空（未部署 / 旧插件缺字段）时禁用控件，但不隐藏该行（仍显示 current 文本）。
     * 候选为空且 current 也为空时展示「未设置」占位文本。
     */
    private void renderSelector(Spinner spinner, List<ModelOption> candidates, List<ModelOption> display, String current) {
        rendering = true;
        display.addAll(candidates);
        boolean hasCurrent = false;
        for (ModelOption option : display) {
            if (option.value.equals(current)) {
                hasCurrent = true;
                break;
            }
        }
        if (!hasCurrent) {
            String label = TextUtils.isEmpty(current) ? dialog.getContext().getString(R.string.agent_ai_unset) : current;
            display.add(0, new ModelOption(current, label));
        }
        List<String> labels = new ArrayList<>();
        for (ModelOption option : display) {
            labels.add(option.label);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(dialog.getContext(),
            android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int index = 0;
        for (int i = 0; i < display.size(); i++) {
            if (display.get(i).value.equals(current)) {
                index = i;
                break;
            }
        }
        spinner.setSelection(index);
        // 候选为空 = 无法切换：禁用控件（apply 冷却结束由 updateEnabledState 统一恢复）
        spinner.setEnabled(!candidates.isEmpty() && !applying);
        // 下拉重建的 onItemSelected 回调在布局后异步触发，延后清除防回环开关
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

    /** 工作目录：当前值只读展示 */
    private void renderCwd() {
        if (TextUtils.isEmpty(currentCwd)) {
            cwdCurrentView.setText(dialog.getContext().getString(R.string.agent_ai_cwd_current, "未设置"));
        } else {
            cwdCurrentView.setText(dialog.getContext().getString(R.string.agent_ai_cwd_current, currentCwd));
        }
        cwdCurrentView.setVisibility(View.VISIBLE);
        // 目录列表打开中：候选来自 209 应答缓存（或老插件 type=3 的 dirs）；
        // 207 op=dirs 请求进行中保持加载态，等 209 应答（不被 type=3 刷新清掉）
        if (cwdListOpen) {
            if (dirsLoading) {
                return;
            }
            updateCwdCandidates();
            renderCwdList();
            if (cwdCandidates.isEmpty()) {
                cwdListContainer.setVisibility(View.GONE);
                cwdListEmptyView.setText(R.string.agent_ai_cwd_list_empty);
                cwdListEmptyView.setVisibility(View.VISIBLE);
            } else {
                cwdListEmptyView.setVisibility(View.GONE);
                cwdListContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 点「切换」：优先用缓存（TTL 60s）；无缓存/已过期则发 207 op=dirs 并显示加载态，
     * 等 209 Agent_Command_Result 应答后渲染候选（失败回退/提示，见 {@link #onDirsRequestFailed()}）。
     */
    private void openCwdList() {
        cwdListOpen = true;
        if (isDirsCacheValid()) {
            // 缓存命中：直接渲染，不发请求
            updateCwdCandidates();
            cwdListLoadingView.setVisibility(View.GONE);
            cwdListEmptyView.setVisibility(View.GONE);
            renderCwdList();
            if (cwdCandidates.isEmpty()) {
                cwdListContainer.setVisibility(View.GONE);
                cwdListEmptyView.setText(R.string.agent_ai_cwd_list_empty);
                cwdListEmptyView.setVisibility(View.VISIBLE);
            } else {
                cwdListContainer.setVisibility(View.VISIBLE);
            }
            updateEnabledState();
            return;
        }
        // 无缓存：显示加载态并请求
        cwdListContainer.setVisibility(View.GONE);
        cwdListEmptyView.setVisibility(View.GONE);
        cwdListLoadingView.setVisibility(View.VISIBLE);
        requestDirs();
        updateEnabledState();
    }

    /** 重建目录候选列表（点某个目录发 207 set /cwd 目录名并关闭列表） */
    private void renderCwdList() {
        cwdListContainer.removeAllViews();
        if (!TextUtils.isEmpty(dirsRoot)) {
            // 209 应答带 root（dirs 的父目录）：列表顶部展示，便于确认候选来源
            TextView header = new TextView(dialog.getContext());
            header.setText(dialog.getContext().getString(R.string.agent_ai_cwd_root, dirsRoot));
            header.setTextSize(11);
            header.setTextColor(dialog.getContext().getResources().getColor(R.color.gray12));
            header.setPadding(dp(12), dp(6), dp(12), dp(2));
            header.setSingleLine(true);
            header.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            cwdListContainer.addView(header);
        }
        for (final String dir : cwdCandidates) {
            TextView item = new TextView(dialog.getContext());
            item.setText("📂 " + dir);
            item.setTextSize(14);
            item.setTextColor(dialog.getContext().getResources().getColor(R.color.gray0));
            item.setPadding(dp(12), dp(8), dp(12), dp(8));
            item.setSingleLine(true);
            item.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            item.setOnClickListener(v -> switchCwd(dir));
            cwdListContainer.addView(item);
        }
        if (dirsTruncated && dirsTotal > cwdCandidates.size()) {
            // 插件截断（上限 3000 条）时提示仅显示前 N 条
            TextView footer = new TextView(dialog.getContext());
            footer.setText(dialog.getContext().getString(R.string.agent_ai_cwd_list_truncated, cwdCandidates.size()));
            footer.setTextSize(11);
            footer.setTextColor(dialog.getContext().getResources().getColor(R.color.gray12));
            footer.setPadding(dp(12), dp(6), dp(12), dp(6));
            cwdListContainer.addView(footer);
        }
    }

    /** 点某个目录：发 207 set /cwd 目录名，关闭列表；当前值随后由 type=3 刷新 */
    private void switchCwd(String dir) {
        if (TextUtils.isEmpty(dir)) {
            return;
        }
        sendCommand("set", "/cwd " + dir);
        // 目录已切换：候选可能变化，作废缓存（下次打开重新按需获取）
        invalidateDirsCache();
        closeCwdList();
    }

    /** 手动输入绝对路径兜底：获取目录失败时仍可切换（发 207 set /cwd <路径>） */
    private void promptManualCwd() {
        new MaterialDialog.Builder(dialog.getContext())
            .title(R.string.agent_ai_cwd_manual_title)
            .input(dialog.getContext().getString(R.string.agent_ai_cwd_manual_hint),
                TextUtils.isEmpty(currentCwd) ? "" : currentCwd, false, (dialog1, input) -> {
                    String path = input != null ? input.toString().trim() : "";
                    if (TextUtils.isEmpty(path)) {
                        return;
                    }
                    sendCommand("set", "/cwd " + path);
                    invalidateDirsCache();
                    closeCwdList();
                })
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .show();
    }

    /** 作废 209 目录缓存（切换目录后候选可能变化，下次打开重新请求） */
    private void invalidateDirsCache() {
        dirsCache.clear();
        dirsCacheAt = 0L;
        dirsTruncated = false;
        dirsTotal = 0;
        dirsRoot = "";
    }

    /** 关闭目录列表（选中后 / 取消）：同时清空 pending 与超时，避免迟到应答影响下次打开 */
    private void closeCwdList() {
        cwdListOpen = false;
        dirsLoading = false;
        pendingDirsRequests.clear();
        handler.removeCallbacks(dirsRequestTimeoutRunnable);
        cwdListLoadingView.setVisibility(View.GONE);
        cwdListContainer.setVisibility(View.GONE);
        cwdListEmptyView.setVisibility(View.GONE);
        updateEnabledState();
    }

    private int dp(float value) {
        return (int) (value * dialog.getContext().getResources().getDisplayMetrics().density + 0.5f);
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
        modelSpinner.setEnabled(enabled);
        effortSpinner.setEnabled(enabled);
        // Agent 模式/工具审批：options 为空（未部署 / 旧插件缺字段）时保持禁用（仅展示 current）
        presetSpinner.setEnabled(enabled && !presetOptions.isEmpty());
        approvalSpinner.setEnabled(enabled && !approvalOptions.isEmpty());
        // 会话模式：候选有本地兜底，始终可切换（仅受操作冷却约束）
        modeSpinner.setEnabled(enabled);
        setRadioGroupEnabled(sandboxGroup, enabled);
        planSwitch.setEnabled(enabled);
        // 目录列表请求进行中禁用「切换」（避免重复发 207）；手动输入兜底始终可用
        cwdSwitchBtn.setEnabled(enabled && !dirsLoading);
        cwdManualBtn.setEnabled(enabled);
        compactBtn.setEnabled(enabled);
        resetBtn.setEnabled(enabled);
        // 销毁按钮不随操作冷却禁用：危险操作始终可点（每次点击都会再弹确认）
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
        // 解绑 209 应答监听（dialog dismiss 后不再消费透明消息）
        ChatManager.Instance().removeOnReceiveMessageListener(receiveMessageListener);
        pendingDirsRequests.clear();
        dirsLoading = false;
        handler.removeCallbacksAndMessages(null);
    }
}
